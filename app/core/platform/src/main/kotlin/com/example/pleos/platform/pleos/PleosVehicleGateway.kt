package com.example.pleos.platform.pleos

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [VehicleGateway] on the Pleos Vehicle SDK 2.3.2.
 *
 * Polls the getters every [POLL_INTERVAL_MS]: listener support is not listed in the Playground
 * console, and polling behaves the same on every vehicle. The charge port has no getter, so it
 * comes from its listener. A value the vehicle does not support stays null.
 *
 * Units, from the emulator measurements in PLEOS_PLATFORM_RESEARCH 11.2: odometer is km; battery
 * energy and capacity come in Wh (77,400 for a 77.4 kWh pack) and are turned into kWh. The range
 * unit is not documented; AAOS uses metres, so a value too large to be kilometres is divided by 1000.
 */
internal class PleosVehicleGateway(
    context: Context,
    scope: CoroutineScope,
) : VehicleGateway {

    private val appContext = context.applicationContext

    override val activeSource: Flow<DataSourceMode> = flowOf(DataSourceMode.REAL)

    @Volatile
    private var lastSnapshot: VehicleSnapshot? = null

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

        launch {
            var cycle = 0
            var capacityWh: Double? = null
            var warnings = emptySet<VehicleWarning>()
            while (isActive) {
                if (capacityWh == null) {
                    capacityWh = read<Float> { ok, fail -> vehicle.getEvBattery().getEvBatteryCapacity(ok, fail) }?.toDouble()
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
    override suspend fun vin(): String? {
        val vehicle = Vehicle(appContext)
        vehicle.initialize()
        return try {
            repeat(VIN_ATTEMPTS) {
                val vin = read<String> { ok, fail -> vehicle.getCarInfo().getVINInfo(ok, fail) }
                if (vin != null) return vin.trim().takeUnless { it.isEmpty() || it.equals("invalid", ignoreCase = true) }
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
    ): VehicleSnapshot {
        val ev = vehicle.getEvBattery()
        val levelWh = read<Float> { ok, fail -> ev.getEvBatteryLevel(ok, fail) }?.toDouble()
        val wattHours = capacityWh == null || capacityWh > WH_THRESHOLD
        val toKwh = { v: Double -> if (wattHours) v / 1000.0 else v }
        val range = read<Float> { ok, fail -> vehicle.getOdometer().getRangeRemainingDistance(ok, fail) }?.toDouble()
        return VehicleSnapshot(
            fuelPercent = read<Int> { ok, fail -> vehicle.getFuel().getFuelPercent(ok, fail) },
            cumulativeDistanceKm = read<Float> { ok, fail -> vehicle.getOdometer().getCurrentDistanceDriven(ok, fail) }?.toDouble(),
            warnings = warnings,
            gear = read<VehicleGear> { ok, fail -> vehicle.getGear().getCurrentGearSelectionState(ok, fail) }?.toDomain(),
            brakePedalPressed = read<Boolean> { ok, fail -> vehicle.getBrake().getCurrentBrakePedalStatus(ok, fail) },
            hvacOn = read<Boolean> { ok, fail -> vehicle.getHVAC().getHvacAcState(ALL_SEATS, ok, fail) },
            rangeRemainingKm = range?.let { if (it > RANGE_METRES_THRESHOLD) it / 1000.0 else it },
            batteryPercent = read<Int> { ok, fail -> ev.getEvBatteryStateOfCharge(ok, fail) }?.toDouble(),
            batteryEnergyKwh = levelWh?.let(toKwh),
            batteryCapacityKwh = capacityWh?.let(toKwh),
            chargePortOpen = portOpen,
        )
    }

    private suspend fun readWarnings(vehicle: Vehicle): Set<VehicleWarning> {
        val safety = vehicle.getSafety()
        fun OilLevelState?.isLow() = this == OilLevelState.LOW || this == OilLevelState.CRITICALLY_LOW
        return buildSet {
            if (read<OilLevelState> { ok, fail -> safety.getEngineOilLevelState(ok, fail) }.isLow()) add(VehicleWarning.ENGINE_OIL)
            if (read<OilLevelState> { ok, fail -> safety.getBrakeFluidLevelState(ok, fail) }.isLow()) add(VehicleWarning.BRAKE_FLUID)
            if (read<OilLevelState> { ok, fail -> safety.getTransmissionOilLevelState(ok, fail) }.isLow()) add(VehicleWarning.TRANSMISSION_OIL)
            if (read<Boolean> { ok, fail -> safety.hasLowWasherFluidWarning(ok, fail) } == true) add(VehicleWarning.WASHER_FLUID)
            if (read<Boolean> { ok, fail -> safety.hasPowerDownWarningIndicator(ok, fail) } == true) add(VehicleWarning.POWER_DOWN)
        }
    }

    /** One SDK getter as a suspend call. Failure, an exception or no answer in time gives null. */
    private suspend fun <T : Any> read(call: (onComplete: (T?) -> Unit, onFailed: (Exception) -> Unit) -> Unit): T? =
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine<T?> { cont ->
                try {
                    call({ value -> if (cont.isActive) cont.resume(value) }, { if (cont.isActive) cont.resume(null) })
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
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
        private const val POLL_INTERVAL_MS = 1_000L
        private const val READ_TIMEOUT_MS = 1_500L
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val WARNING_EVERY_N_POLLS = 10
        private const val VIN_ATTEMPTS = 3
        private const val VIN_RETRY_MS = 500L

        /** A pack above 1,000 is in Wh; no car battery is 1,000 kWh. */
        private const val WH_THRESHOLD = 1_000.0

        /** No car has 3,000 km of range, so a larger value is metres. */
        private const val RANGE_METRES_THRESHOLD = 3_000.0

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
