package com.example.pleos.domain.model

/** Everything the dashboard shows, combined from the vehicle and AAOS sources. */
data class DashboardData(
    val vehicle: VehicleSnapshot,
    val car: CarProperties,
    /** AAOS range when available, otherwise fuel % × tank capacity × current economy. */
    val estimatedRangeKm: Double?,
    val source: DataSourceMode,
)
