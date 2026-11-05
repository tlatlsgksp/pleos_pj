package com.example.pleos.domain.model

import java.time.Instant

enum class SessionKind {
    /** The app was running and sampled the drive. */
    MEASURED,

    /** Rebuilt from the odometer difference while the app was off. Distance and energy only, no samples. */
    GAP,
}

/**
 * One drive from start to stop (FEATURE_LOGIC 2.4). Energy is kWh for EV and L for fuel vehicles.
 * `end == null` means the session is still open.
 */
data class DriveSession(
    val id: String,
    val vehicleId: String,
    val start: VehicleStamp,
    val end: VehicleStamp?,
    val distanceKm: Double,
    val energyUsed: Double?,
    val idleSeconds: Long,
    val idleEnergyUsed: Double?,
    /** Toll the navigation estimated for the route. */
    val tollWon: Long? = null,
    val destinationName: String? = null,
    val kind: SessionKind,
)

/** One sample of an open session. Read by cause analysis and replay. */
data class DriveSample(
    val sessionId: String,
    val at: Instant,
    val speedKmh: Double?,
    val gear: Gear?,
    /** Remaining energy: kWh for EV, L for fuel. */
    val energyRemaining: Double?,
    val hvacOn: Boolean?,
    val brakePedalPressed: Boolean?,
    val latitude: Double?,
    val longitude: Double?,
)
