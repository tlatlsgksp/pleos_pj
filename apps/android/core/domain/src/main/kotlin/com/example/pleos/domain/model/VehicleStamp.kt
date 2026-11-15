package com.example.pleos.domain.model

import java.time.Instant

enum class StampSource {
    /** Read from the Vehicle SDK. */
    VEHICLE,

    /** Produced by the demo scenario. */
    DEMO,

    /** No vehicle value was available; taken from the last known session end or the baseline. */
    MANUAL,
}

/**
 * Values read from the vehicle when a record or session was made. Attached to every record and
 * session from the start because they cannot be reconstructed later. The user cannot edit them;
 * they are what makes the maintenance history credible (FEATURE_LOGIC 2.2).
 */
data class VehicleStamp(
    val odometerKm: Double,
    val vinHash: String?,
    val capturedAt: Instant,
    val source: StampSource,
)
