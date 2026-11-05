package com.example.pleos.domain.drive

import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleSnapshot
import java.time.Instant

/** A finished charge (EV) or refuel (fuel) that passed the minimum-increase check. */
data class EnergyEvent(
    val kind: EnergyType,
    val startedAt: Instant,
    val endedAt: Instant,
    /** kWh for EV, L for fuel. null when the fuel tank capacity is unknown. */
    val amount: Double?,
    /** Battery % or fuel % before and after. */
    val levelBefore: Double?,
    val levelAfter: Double?,
    val odometerKm: Double?,
)

/**
 * FEATURE_LOGIC L2-4. Feed every vehicle snapshot in order; returns an event when a charge or a
 * refuel ends. Pure and single-threaded; the caller owns the instance.
 *
 * EV: starts when the charge port opens, or when battery energy rises while stationary (the port
 * signal may be missing). Ends when the port closes after having been open, or the gear leaves P/N.
 * Fuel: starts when fuel % rises while stationary or the since-refill trip resets. Ends at gear D/R.
 */
class EnergyEventDetector(
    private val energyType: EnergyType,
    /** Needed to turn fuel % into litres. */
    private val tankCapacityL: Double? = null,
    private val minChargeKwh: Double = DEFAULT_MIN_CHARGE_KWH,
    private val minRefuelPercent: Double = DEFAULT_MIN_REFUEL_PERCENT,
) {
    private var previous: VehicleSnapshot? = null
    private var active: Active? = null

    private data class Active(
        val startedAt: Instant,
        val energyBefore: Double,
        val percentBefore: Double?,
        var energyMax: Double,
        var percentMax: Double?,
        var portSeenOpen: Boolean,
    )

    val isActive: Boolean get() = active != null

    fun onSnapshot(snapshot: VehicleSnapshot, speedKmh: Double?, at: Instant): EnergyEvent? {
        val prev = previous
        previous = snapshot
        val energy = energyOf(snapshot) ?: return null
        val prevEnergy = prev?.let(::energyOf)
        val stationary = isStationary(snapshot.gear, speedKmh)

        val current = active
        if (current == null) {
            val rose = prevEnergy != null && energy > prevEnergy + RISE_EPSILON
            val portOpened = energyType == EnergyType.EV && snapshot.chargePortOpen == true && prev?.chargePortOpen != true
            val tripReset = energyType == EnergyType.FUEL && (prev?.tripSinceRefillKm ?: 0.0) > 1.0 &&
                (snapshot.tripSinceRefillKm ?: Double.MAX_VALUE) < 0.1
            if (portOpened || (stationary && (rose || tripReset))) {
                // When the rise is what we noticed, the level before is the previous sample.
                val base = if (rose && !portOpened) prevEnergy!! else energy
                val basePercent = if (rose && !portOpened) percentOf(prev!!) else percentOf(snapshot)
                active = Active(at, base, basePercent, energy, percentOf(snapshot), snapshot.chargePortOpen == true)
            }
            return null
        }

        if (energy > current.energyMax) current.energyMax = energy
        percentOf(snapshot)?.let { p -> if (p > (current.percentMax ?: Double.NEGATIVE_INFINITY)) current.percentMax = p }
        if (snapshot.chargePortOpen == true) current.portSeenOpen = true

        val portClosed = current.portSeenOpen && snapshot.chargePortOpen == false
        val moving = snapshot.gear == Gear.DRIVE || snapshot.gear == Gear.REVERSE
        if (!portClosed && !moving) return null

        active = null
        val increase = current.energyMax - current.energyBefore
        val enough = when (energyType) {
            EnergyType.EV -> increase >= minChargeKwh
            EnergyType.FUEL -> increase >= minRefuelPercent
        }
        if (!enough) return null
        return EnergyEvent(
            kind = energyType,
            startedAt = current.startedAt,
            endedAt = at,
            amount = when (energyType) {
                EnergyType.EV -> increase
                EnergyType.FUEL -> tankCapacityL?.let { increase / 100.0 * it }
            },
            levelBefore = current.percentBefore,
            levelAfter = current.percentMax,
            odometerKm = snapshot.cumulativeDistanceKm,
        )
    }

    /** kWh for EV (falls back to % x capacity), fuel % for fuel vehicles. */
    private fun energyOf(s: VehicleSnapshot): Double? = when (energyType) {
        EnergyType.EV -> s.batteryEnergyKwh
            ?: s.batteryPercent?.let { p -> s.batteryCapacityKwh?.let { it * p / 100.0 } }
        EnergyType.FUEL -> s.fuelPercent?.toDouble()
    }

    private fun percentOf(s: VehicleSnapshot): Double? = when (energyType) {
        EnergyType.EV -> s.batteryPercent
            ?: s.batteryEnergyKwh?.let { e -> s.batteryCapacityKwh?.takeIf { it > 0 }?.let { e / it * 100.0 } }
        EnergyType.FUEL -> s.fuelPercent?.toDouble()
    }

    companion object {
        const val DEFAULT_MIN_CHARGE_KWH = 1.0
        const val DEFAULT_MIN_REFUEL_PERCENT = 5.0
        private const val RISE_EPSILON = 0.05

        /** Gear P/N counts as stationary; without a gear signal, speed 0 does. */
        fun isStationary(gear: Gear?, speedKmh: Double?): Boolean = when (gear) {
            Gear.PARK, Gear.NEUTRAL -> true
            Gear.DRIVE, Gear.REVERSE -> speedKmh == 0.0
            Gear.UNKNOWN, null -> speedKmh == null || speedKmh == 0.0
        }
    }
}
