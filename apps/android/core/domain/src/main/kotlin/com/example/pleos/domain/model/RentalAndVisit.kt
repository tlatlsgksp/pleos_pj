package com.example.pleos.domain.model

import java.time.Instant

/** A period the car was lent out. Records made inside it carry its id (FEATURE_LOGIC L1-4). */
data class RentalPeriod(
    val id: String,
    val vehicleId: String,
    val start: Instant,
    val end: Instant?,
    val startStamp: VehicleStamp,
    val endStamp: VehicleStamp?,
)

/** Arrival at a repair shop not yet turned into a maintenance record (FEATURE_LOGIC L4-4). */
data class PendingVisit(
    val id: String,
    val vehicleId: String,
    val shopName: String,
    val arrivedAt: Instant,
    val arrivedStamp: VehicleStamp,
)
