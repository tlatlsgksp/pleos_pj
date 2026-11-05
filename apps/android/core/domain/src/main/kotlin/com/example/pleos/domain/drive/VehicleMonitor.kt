package com.example.pleos.domain.drive

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.IdGenerator
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.ledger.RecordFactory
import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.DriveSession
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.SessionKind
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleStamp
import com.example.pleos.domain.repository.DriveSessionRepository
import com.example.pleos.domain.repository.RecordRepository
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.repository.VehicleRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Turns the live vehicle signal into stored facts: registers the vehicle on the first odometer
 * reading, cuts drive sessions (L2-1, L2-2) and saves detected charges and refuels as DRAFT
 * records (L2-4, L1-1). Runs for the life of the app process; call [run] from an app-wide scope.
 *
 * Switching between the demo and the real vehicle resets the detectors, so the odometer jump
 * between the two sources is not taken for a drive.
 */
@Singleton
class VehicleMonitor @Inject constructor(
    private val gateway: VehicleGateway,
    private val car: CarPropertySource,
    private val vehicles: VehicleRepository,
    private val sessions: DriveSessionRepository,
    private val records: RecordRepository,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    private var vehicle: Vehicle? = null
    private var source: DataSourceMode? = null
    private var tracker: DriveSessionTracker? = null
    private var detector: EnergyEventDetector? = null
    private var openSession: DriveSession? = null

    suspend fun run() {
        combine(gateway.snapshot, car.properties, gateway.activeSource) { s, p, m -> Triple(s, p, m) }
            .collect { (snapshot, properties, mode) -> onSignal(snapshot, properties, mode) }
    }

    /** Closes an open drive, e.g. when the app is going away. */
    suspend fun shutdown() {
        tracker?.onShutdown(Instant.now(clock))?.let { handle(it, null) }
    }

    internal suspend fun onSignal(snapshot: VehicleSnapshot, properties: CarProperties, mode: DataSourceMode) {
        val now = Instant.now(clock)
        val vehicle = ensureVehicle(snapshot, properties, now) ?: return

        if (mode != source) {
            tracker?.onShutdown(now)?.let { handle(it, null) }
            source = mode
            closeStaleSessions(vehicle, now)
            val last = sessions.lastEnded(vehicle.id)
            tracker = DriveSessionTracker(lastEndOdometerKm = last?.end?.odometerKm ?: vehicle.baselineOdometerKm)
            detector = EnergyEventDetector(vehicle.energyType, vehicle.tankCapacityL ?: properties.fuelCapacityL)
        }
        val tracker = checkNotNull(tracker)
        val detector = checkNotNull(detector)

        val wasCharging = detector.isActive
        val energyEvent = detector.onSnapshot(snapshot, properties.speedKmh, now)
        if (!wasCharging && detector.isActive) {
            tracker.onEnergyEventStarted(now)?.let { handle(it, snapshot) }
        }
        tracker.onSnapshot(snapshot, energyOf(snapshot, vehicle, properties), now).forEach { handle(it, snapshot) }
        energyEvent?.let { saveDraft(it, vehicle, snapshot, now) }
    }

    private suspend fun ensureVehicle(snapshot: VehicleSnapshot, properties: CarProperties, now: Instant): Vehicle? {
        vehicle?.let { return it }
        vehicles.current()?.let { vehicle = it; return it }
        val odometer = snapshot.cumulativeDistanceKm ?: return null
        val isEv = snapshot.batteryEnergyKwh != null || snapshot.batteryPercent != null
        val created = Vehicle(
            id = ids.newId(),
            energyType = if (isEv) EnergyType.EV else EnergyType.FUEL,
            vinHash = gateway.vin()?.takeIf { it.isNotBlank() }?.let(RecordFactory::sha256),
            tankCapacityL = if (isEv) null else properties.fuelCapacityL,
            batteryCapacityKwh = if (isEv) snapshot.batteryCapacityKwh else null,
            baselineOdometerKm = odometer,
            baselineAt = now,
        )
        vehicles.save(created)
        vehicle = created
        return created
    }

    /** Sessions left open by a previous process are closed at their start values. */
    private suspend fun closeStaleSessions(vehicle: Vehicle, now: Instant) {
        sessions.open(vehicle.id).forEach { stale ->
            sessions.upsert(stale.copy(end = stale.start.copy(capturedAt = now, source = StampSource.MANUAL)))
        }
    }

    private suspend fun handle(event: SessionEvent, snapshot: VehicleSnapshot?) {
        val vehicle = vehicle ?: return
        when (event) {
            is SessionEvent.Started -> {
                val startOdometer = event.odometerKm ?: snapshot?.cumulativeDistanceKm ?: vehicle.baselineOdometerKm
                if (event.gapKm != null) {
                    val last = sessions.lastEnded(vehicle.id)
                    val gapStart = VehicleStamp(
                        odometerKm = startOdometer - event.gapKm,
                        vinHash = vehicle.vinHash,
                        capturedAt = last?.end?.capturedAt ?: vehicle.baselineAt,
                        source = StampSource.MANUAL,
                    )
                    sessions.upsert(
                        DriveSession(
                            id = ids.newId(), vehicleId = vehicle.id,
                            start = gapStart, end = stamp(vehicle, startOdometer, event.at),
                            distanceKm = event.gapKm, energyUsed = event.gapEnergy,
                            idleSeconds = 0, idleEnergyUsed = null, kind = SessionKind.GAP,
                        ),
                    )
                }
                val session = DriveSession(
                    id = ids.newId(), vehicleId = vehicle.id,
                    start = stamp(vehicle, startOdometer, event.at), end = null,
                    distanceKm = 0.0, energyUsed = null,
                    idleSeconds = 0, idleEnergyUsed = null, kind = SessionKind.MEASURED,
                )
                sessions.upsert(session)
                openSession = session
            }
            is SessionEvent.Ended -> {
                val open = openSession ?: return
                openSession = null
                val endOdometer = event.endOdometerKm ?: open.start.odometerKm + event.distanceKm
                sessions.upsert(
                    open.copy(
                        end = stamp(vehicle, endOdometer, event.endedAt),
                        distanceKm = event.distanceKm,
                        energyUsed = event.energyUsed,
                    ),
                )
            }
        }
    }

    /**
     * Saves a detected charge or refuel as a DRAFT. A draft of the same kind from the last
     * [DRAFT_MERGE_WINDOW] is updated instead, so an interrupted charge stays one record (L1-1).
     */
    private suspend fun saveDraft(event: EnergyEvent, vehicle: Vehicle, snapshot: VehicleSnapshot, now: Instant) {
        val existing = records.latestDraft(vehicle.id, RecordType.ENERGY, now.minus(DRAFT_MERGE_WINDOW))
        val previous = existing?.detail as? RecordDetail.Energy
        val merged = if (previous != null) {
            event.copy(
                startedAt = minOf(event.startedAt, existing.occurredAt),
                amount = if (previous.amount != null && event.amount != null) previous.amount + event.amount else event.amount,
                levelBefore = previous.levelBefore ?: event.levelBefore,
            )
        } else {
            event
        }
        val unitPrice = if (vehicle.energyType == EnergyType.EV) settings.chargeUnitPriceWon.first() else null
        val fallback = sessions.lastEnded(vehicle.id)?.end?.odometerKm ?: vehicle.baselineOdometerKm
        val stamp = RecordFactory.stamp(snapshot.cumulativeDistanceKm, vehicle.vinHash, now, stampSource(), fallback)
        records.upsert(RecordFactory.energyDraft(existing?.id ?: ids.newId(), vehicle, merged, stamp, unitPrice))
    }

    private fun stamp(vehicle: Vehicle, odometerKm: Double, at: Instant) =
        VehicleStamp(odometerKm, vehicle.vinHash, at, stampSource())

    private fun stampSource(): StampSource =
        if (source == DataSourceMode.REAL) StampSource.VEHICLE else StampSource.DEMO

    /** kWh for EV; litres for fuel when the tank size is known. */
    private fun energyOf(s: VehicleSnapshot, vehicle: Vehicle, properties: CarProperties): Double? = when (vehicle.energyType) {
        EnergyType.EV -> s.batteryEnergyKwh
            ?: s.batteryPercent?.let { p -> (s.batteryCapacityKwh ?: vehicle.batteryCapacityKwh)?.let { it * p / 100.0 } }
        EnergyType.FUEL -> s.fuelPercent?.let { p -> (vehicle.tankCapacityL ?: properties.fuelCapacityL)?.let { it * p / 100.0 } }
    }

    companion object {
        /** [기본값] FEATURE_LOGIC L1-1. */
        val DRAFT_MERGE_WINDOW: Duration = Duration.ofMinutes(30)
    }
}
