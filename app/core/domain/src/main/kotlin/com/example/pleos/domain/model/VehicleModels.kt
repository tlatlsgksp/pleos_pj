package com.example.pleos.domain.model

/** Drive mode as reported by the vehicle. UNKNOWN covers values the gateway cannot map. */
enum class DrivingMode { ECO, NORMAL, SPORT, SNOW, CUSTOM, UNKNOWN }

/** Maintenance and safety warnings. Each maps to one Vehicle SDK safety signal. */
enum class VehicleWarning {
    ENGINE_OIL,
    BRAKE_FLUID,
    TRANSMISSION_OIL,
    LOW_TIRE_PRESSURE,
    WASHER_FLUID,
    POWER_DOWN,
    AIRBAG_FAULT,
    SEAT_BELT,
}

/** Features whose support varies by vehicle model. Gateways resolve these with checkXxxCapabilities. */
enum class VehicleFeature {
    FUEL_PERCENT,
    FUEL_ECONOMY,
    TRIP_DISTANCE,
    DRIVING_MODE,
    SAFETY_WARNINGS,
    AIRBAG_DEPLOYED,
    VIN,
}

enum class Capability {
    UNSUPPORTED,
    READ,
    WRITE,
    READ_WRITE;

    val canRead: Boolean get() = this == READ || this == READ_WRITE
}

/**
 * Latest known vehicle state. Units are fixed: km, L, %, km/L.
 * `null` means unknown or unsupported on this vehicle; the UI must handle it.
 */
data class VehicleSnapshot(
    val fuelPercent: Int? = null,
    val instantFuelEconomyKmPerL: Double? = null,
    val currentFuelEconomyKmPerL: Double? = null,
    val tripSinceRefillKm: Double? = null,
    val cumulativeDistanceKm: Double? = null,
    val drivingMode: DrivingMode? = null,
    val warnings: Set<VehicleWarning> = emptySet(),
    val airbagDeployed: Boolean? = null,
)

/** AAOS standard properties that the Pleos Vehicle SDK does not expose. */
data class CarProperties(
    val speedKmh: Double? = null,
    val ignitionOn: Boolean? = null,
    val rangeRemainingKm: Double? = null,
    val fuelCapacityL: Double? = null,
)

/** A refuel detected from vehicle signals, waiting for the user to add the amount paid (won). */
data class RefuelDraft(
    val detectedAtEpochMillis: Long,
    val odometerKm: Double?,
    val fuelPercentBefore: Int?,
    val fuelPercentAfter: Int?,
    val estimatedLiters: Double?,
)

/** Where vehicle data comes from. DEMO is the Fake gateways driven by a scenario. */
enum class DataSourceMode { REAL, DEMO }
