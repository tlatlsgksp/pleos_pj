package com.example.pleos.platform.pleos

import android.content.Context
import android.content.Intent
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.Capability
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleFeature
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleWarning
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [VehicleGateway] on the Pleos Vehicle SDK 2.3.2.
 *
 * Polls the getters every [POLL_INTERVAL_MS]: listener support is not listed in the Playground
 * console, and polling behaves the same on every vehicle. Calls are made on the main thread, as
 * the SDK sample does. A read that fails or gets no answer is logged under [TAG]; when every read
 * times out, the vehicle service is not active yet (on the emulator a reboot fixed it). The charge port has no getter, so it
 * comes from its listener. A value the vehicle does not support stays null.
 *
 * Units, from the emulator measurements in PLEOS_PLATFORM_RESEARCH 11.2: odometer is km; battery
 * energy and capacity come in Wh (77,400 for a 77.4 kWh pack) and are turned into kWh. Range comes
 * in metres, as in AAOS RANGE_REMAINING and the Virtual Console (remaining_range_m).
 */
internal class PleosVehicleGateway(
    context: Context,
    scope: CoroutineScope,
) : VehicleGateway {

    private val appContext = context.applicationContext

    override val activeSource: Flow<DataSourceMode> = flowOf(DataSourceMode.REAL)

    @Volatile
    private var lastSnapshot: VehicleSnapshot? = null

    /** Reads the vehicle answered with "not supported". Only touched on the main thread. */
    private val unsupported = mutableSetOf<String>()

    override val snapshot: Flow<VehicleSnapshot> = callbackFlow {
        val vehicle = Vehicle(appContext)
        vehicle.initialize()

        val portOpen = MutableStateFlow<Boolean?>(null)
        val portListener = object : EvChargePortOpenListener {
            override fun onEvChargePortOpenUpdated(open: Boolean?) {
                portOpen.value = open
            }

            override fun onFailed(e: Exception) = Unit
        }
        runCatching { vehicle.getEvBattery().registerEvChargePortOpen(portListener) }

        launch(Dispatchers.Main) {
            var cycle = 0
            var capacityWh: Double? = null
            var warnings = emptySet<VehicleWarning>()
            while (isActive) {
                if (capacityWh == null) {
                    capacityWh = read<Float>("getEvBatteryCapacity") { ok, fail -> vehicle.getEvBattery().getEvBatteryCapacity(ok, fail) }?.toDouble()
                }
                if (cycle % WARNING_EVERY_N_POLLS == 0) warnings = readWarnings(vehicle)
                val s = readSnapshot(vehicle, capacityWh, portOpen.value, warnings)
                lastSnapshot = s
                send(s)
                cycle++
                delay(POLL_INTERVAL_MS)
            }
        }

        awaitClose {
            runCatching { vehicle.getEvBattery().unregisterEvChargePortOpen(portListener) }
            runCatching { vehicle.release() }
        }
    }
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    override suspend fun capabilities(): Map<VehicleFeature, Capability> {
        val s = lastSnapshot ?: return emptyMap()
        fun cap(value: Any?) = if (value != null) Capability.READ else Capability.UNSUPPORTED
        return mapOf(
            VehicleFeature.FUEL_PERCENT to cap(s.fuelPercent),
            VehicleFeature.EV_BATTERY to cap(s.batteryEnergyKwh ?: s.batteryPercent),
            VehicleFeature.CHARGE_PORT to cap(s.chargePortOpen),
            VehicleFeature.GEAR to cap(s.gear),
            VehicleFeature.BRAKE_PEDAL to cap(s.brakePedalPressed),
            VehicleFeature.HVAC to cap(s.hvacOn),
            VehicleFeature.RANGE_REMAINING to cap(s.rangeRemainingKm),
        )
    }

    /** The emulator answers "invalid"; that and blanks count as no VIN. */
    override suspend fun vin(): String? = withContext(Dispatchers.Main) {
        val vehicle = Vehicle(appContext)
        vehicle.initialize()
        try {
            repeat(VIN_ATTEMPTS) {
                val vin = read<String>("getVINInfo") { ok, fail -> vehicle.getCarInfo().getVINInfo(ok, fail) }
                if (vin != null) return@withContext vin.trim().takeUnless { it.isEmpty() || it.equals("invalid", ignoreCase = true) }
                delay(VIN_RETRY_MS)
            }
            null
        } finally {
            runCatching { vehicle.release() }
        }
    }

    private suspend fun readSnapshot(
        vehicle: Vehicle,
        capacityWh: Double?,
        portOpen: Boolean?,
        warnings: Set<VehicleWarning>,
    ): VehicleSnapshot = coroutineScope {
        // All reads in parallel: one slow answer must not delay the others, or short events
        // (a charge port opening) fall between two samples.
        val ev = vehicle.getEvBattery()
        val level = async { read<Float>("getEvBatteryLevel") { ok, fail -> ev.getEvBatteryLevel(ok, fail) } }
        val range = async { read<Float>("getRangeRemainingDistance") { ok, fail -> vehicle.getOdometer().getRangeRemainingDistance(ok, fail) } }
        val fuel = async { read<Int>("getFuelPercent") { ok, fail -> vehicle.getFuel().getFuelPercent(ok, fail) } }
        val odometer = async { read<Float>("getCurrentDistanceDriven") { ok, fail -> vehicle.getOdometer().getCurrentDistanceDriven(ok, fail) } }
        val gear = async { read<VehicleGear>("getCurrentGearSelectionState") { ok, fail -> vehicle.getGear().getCurrentGearSelectionState(ok, fail) } }
        val brake = async { read<Boolean>("getCurrentBrakePedalStatus") { ok, fail -> vehicle.getBrake().getCurrentBrakePedalStatus(ok, fail) } }
        val hvac = async { read<Boolean>("getHvacAcState") { ok, fail -> vehicle.getHVAC().getHvacAcState(ALL_SEATS, ok, fail) } }
        val soc = async { read<Int>("getEvBatteryStateOfCharge") { ok, fail -> ev.getEvBatteryStateOfCharge(ok, fail) } }

        val wattHours = capacityWh == null || capacityWh > WH_THRESHOLD
        val toKwh = { v: Double -> if (wattHours) v / 1000.0 else v }
        VehicleSnapshot(
            fuelPercent = fuel.await(),
            cumulativeDistanceKm = odometer.await()?.toDouble(),
            warnings = warnings,
            gear = gear.await()?.toDomain(),
            brakePedalPressed = brake.await(),
            hvacOn = hvac.await(),
            rangeRemainingKm = range.await()?.toDouble()?.let { it / 1000.0 },
            batteryPercent = soc.await()?.toDouble(),
            batteryEnergyKwh = level.await()?.toDouble()?.let(toKwh),
            batteryCapacityKwh = capacityWh?.let(toKwh),
            chargePortOpen = portOpen,
        ).also {
            Log.d(TAG, "snapshot odo=${it.cumulativeDistanceKm} kWh=${it.batteryEnergyKwh} soc=${it.batteryPercent} port=${it.chargePortOpen} gear=${it.gear} range=${it.rangeRemainingKm}")
        }
    }

    private suspend fun readWarnings(vehicle: Vehicle): Set<VehicleWarning> {
        val safety = vehicle.getSafety()
        fun OilLevelState?.isLow() = this == OilLevelState.LOW || this == OilLevelState.CRITICALLY_LOW
        return buildSet {
            if (read<OilLevelState>("getEngineOilLevelState") { ok, fail -> safety.getEngineOilLevelState(ok, fail) }.isLow()) add(VehicleWarning.ENGINE_OIL)
            if (read<OilLevelState>("getBrakeFluidLevelState") { ok, fail -> safety.getBrakeFluidLevelState(ok, fail) }.isLow()) add(VehicleWarning.BRAKE_FLUID)
            if (read<OilLevelState>("getTransmissionOilLevelState") { ok, fail -> safety.getTransmissionOilLevelState(ok, fail) }.isLow()) add(VehicleWarning.TRANSMISSION_OIL)
            if (read<Boolean>("hasLowWasherFluidWarning") { ok, fail -> safety.hasLowWasherFluidWarning(ok, fail) } == true) add(VehicleWarning.WASHER_FLUID)
            if (read<Boolean>("hasPowerDownWarningIndicator") { ok, fail -> safety.hasPowerDownWarningIndicator(ok, fail) } == true) add(VehicleWarning.POWER_DOWN)
        }
    }

    /** One SDK getter as a suspend call. Failure, an exception or no answer in time gives null. */
    private suspend fun <T : Any> read(
        name: String,
        call: (onComplete: (T?) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
    ): T? {
        if (name in unsupported) return null
        val value = withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine<Result<T?>> { cont ->
                try {
                    call({ if (cont.isActive) cont.resume(Result.success(it)) }, { if (cont.isActive) cont.resume(Result.failure(it)) })
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
            }
        }
        when {
            value == null -> Log.d(TAG, "$name: no answer in ${READ_TIMEOUT_MS}ms")
            value.isFailure -> {
                val error = value.exceptionOrNull()
                Log.d(TAG, "$name: $error")
                // The car will not start supporting it later; stop asking (saves a call per poll).
                if (error?.message?.contains("not supported") == true) unsupported += name
            }
        }
        return value?.getOrNull()
    }

    private fun VehicleGear.toDomain(): Gear = when (this) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        // D and the manual gears 1..9 all mean moving forward.
        else -> Gear.DRIVE
    }

    companion object {
        private const val TAG = "PleosVehicle"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val READ_TIMEOUT_MS = 1_500L
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val WARNING_EVERY_N_POLLS = 10
        private const val VIN_ATTEMPTS = 3
        private const val VIN_RETRY_MS = 500L

        /** A pack above 1,000 is in Wh; no car battery is 1,000 kWh. */
        private const val WH_THRESHOLD = 1_000.0

        /** The emulator only accepts the whole cabin for HVAC (research notes 11.2). */
        private val ALL_SEATS = SeatArea(
            SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT,
            SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT,
        )

        private const val VEHICLE_SERVICE_ACTION = "ai.pleos.playground.vehicle.VEHICLE_START"

        /** True when the Pleos vehicle service is installed, i.e. on a Pleos car or emulator. */
        fun isAvailable(context: Context): Boolean =
            context.packageManager.queryIntentServices(Intent(VEHICLE_SERVICE_ACTION), 0).isNotEmpty()
    }
}
