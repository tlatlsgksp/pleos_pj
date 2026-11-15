package com.example.pleos.domain.drive

import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleSnapshot
import java.time.Duration
import java.time.Instant

sealed interface SessionEvent {
    /**
     * A drive started. [gapKm] is set when the odometer moved while the app was off by more than
     * the gap threshold; the caller stores that distance as a GAP session first (L2-2).
     */
    data class Started(
        val at: Instant,
        val odometerKm: Double?,
        val energy: Double?,
        val gapKm: Double?,
        val gapEnergy: Double?,
    ) : SessionEvent

    data class Ended(
        val startedAt: Instant,
        val endedAt: Instant,
        val startOdometerKm: Double?,
        val endOdometerKm: Double?,
        val distanceKm: Double,
        /** Energy drop over the session: kWh or fuel %. null if a value was missing or it rose. */
        val energyUsed: Double?,
        val reason: EndReason,
    ) : SessionEvent

    enum class EndReason { PARKED, ENERGY_EVENT, SHUTDOWN }
}

/**
 * FEATURE_LOGIC L2-1 and L2-2. There is no ignition API, so a session starts when the gear leaves P
 * for D/R or the odometer rises, and ends after the gear stays in P for [parkTimeout]. The energy
 * value is whatever the caller passes (kWh for EV, fuel % or L for fuel), so the tracker is type-free.
 */
class DriveSessionTracker(
    /** Odometer at the end of the last stored session, to detect distance driven while the app was off. */
    lastEndOdometerKm: Double?,
    lastEndEnergy: Double? = null,
    private val parkTimeout: Duration = DEFAULT_PARK_TIMEOUT,
    private val gapThresholdKm: Double = DEFAULT_GAP_THRESHOLD_KM,
) {
    private var lastEndOdometer = lastEndOdometerKm
    private var lastEndEnergy = lastEndEnergy
    private var previous: VehicleSnapshot? = null
    private var open: Open? = null
    private var parkedSince: Instant? = null
    private var lastOdometer: Double? = null
    private var lastEnergy: Double? = null

    private data class Open(val at: Instant, val odometer: Double?, val energy: Double?)

    val isOpen: Boolean get() = open != null

    fun onSnapshot(snapshot: VehicleSnapshot, energy: Double?, at: Instant): List<SessionEvent> {
        val prev = previous
        previous = snapshot
        val odometer = snapshot.cumulativeDistanceKm
        val events = mutableListOf<SessionEvent>()

        if (open != null) {
            lastOdometer = odometer ?: lastOdometer
            lastEnergy = energy ?: lastEnergy
        }

        val current = open
        if (current == null) {
            val leftPark = snapshot.gear == Gear.DRIVE || snapshot.gear == Gear.REVERSE
            val gearStarted = leftPark && prev?.gear != snapshot.gear
            val odometerRose = odometer != null && prev?.cumulativeDistanceKm != null &&
                odometer > prev.cumulativeDistanceKm
            if (gearStarted || odometerRose) {
                // On an odometer-triggered start the drive began at the previous value.
                val startOdometer = if (odometerRose && !gearStarted) prev!!.cumulativeDistanceKm else odometer
                events += start(at, startOdometer, energy)
            }
        } else {
            if (snapshot.gear == Gear.PARK) {
                val since = parkedSince ?: at.also { parkedSince = it }
                if (Duration.between(since, at) >= parkTimeout) {
                    events += end(since, SessionEvent.EndReason.PARKED)
                }
            } else {
                parkedSince = null
            }
        }
        return events
    }

    /** A charge or refuel started: close the drive so charging does not count against efficiency. */
    fun onEnergyEventStarted(at: Instant): SessionEvent? =
        if (open != null) end(at, SessionEvent.EndReason.ENERGY_EVENT) else null

    /** App or power going down. */
    fun onShutdown(at: Instant): SessionEvent? =
        if (open != null) end(at, SessionEvent.EndReason.SHUTDOWN) else null

    private fun start(at: Instant, odometer: Double?, energy: Double?): SessionEvent.Started {
        val gapKm = if (odometer != null && lastEndOdometer != null) {
            (odometer - lastEndOdometer!!).takeIf { it > gapThresholdKm }
        } else {
            null
        }
        val gapEnergy = if (gapKm != null && energy != null && lastEndEnergy != null) {
            (lastEndEnergy!! - energy).takeIf { it > 0 }
        } else {
            null
        }
        open = Open(at, odometer, energy)
        parkedSince = null
        lastOdometer = odometer
        lastEnergy = energy
        return SessionEvent.Started(at, odometer, energy, gapKm, gapEnergy)
    }

    private fun end(at: Instant, reason: SessionEvent.EndReason): SessionEvent.Ended {
        val o = checkNotNull(open)
        open = null
        parkedSince = null
        val endOdometer = lastOdometer ?: o.odometer
        val endEnergy = lastEnergy
        val distance = if (endOdometer != null && o.odometer != null) (endOdometer - o.odometer).coerceAtLeast(0.0) else 0.0
        val used = if (o.energy != null && endEnergy != null) (o.energy - endEnergy).takeIf { it >= 0 } else null
        lastEndOdometer = endOdometer ?: lastEndOdometer
        lastEndEnergy = endEnergy ?: lastEndEnergy
        return SessionEvent.Ended(o.at, at, o.odometer, endOdometer, distance, used, reason)
    }

    companion object {
        val DEFAULT_PARK_TIMEOUT: Duration = Duration.ofMinutes(3)
        const val DEFAULT_GAP_THRESHOLD_KM = 0.5
    }
}
