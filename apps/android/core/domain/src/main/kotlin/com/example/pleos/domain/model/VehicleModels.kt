package com.example.pleos.domain.model

/** Drive mode as reported by the vehicle. UNKNOWN covers values the gateway cannot map. */
enum class DrivingMode { ECO, NORMAL, SPORT, SNOW, CUSTOM, UNKNOWN }

/** Gear position. UNKNOWN covers values the gateway cannot map. */
enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

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

    /** EV battery: state of charge, energy and capacity. */
    EV_BATTERY,
    CHARGE_PORT,
    GEAR,
    BRAKE_PEDAL,
    HVAC,
    RANGE_REMAINING,
}

enum class Capability {
    UNSUPPORTED,
    READ,
    WRITE,
    READ_WRITE;

    val canRead: Boolean get() = this == READ || this == READ_WRITE
}

/**
 * Latest known vehicle state. Units are fixed: km, L, %, km/L, kWh.
 * Gateways convert units (the emulator reports battery energy in Wh; here it is kWh).
 * `null` means unknown or unsupported on this vehicle; the UI must handle it.
 *
 * The emulator vehicle is an EV (research notes 11.2): fuel fields stay null there, and EV fields
 * stay null on a fuel vehicle. [cumulativeDistanceKm] is the total odometer that every record and
 * session is stamped with.
 */
data class VehicleSnapshot(
    // Fuel vehicle
    val fuelPercent: Int? = null,
    val instantFuelEconomyKmPerL: Double? = null,
    val currentFuelEconomyKmPerL: Double? = null,
    val tripSinceRefillKm: Double? = null,

    // Shared
    val cumulativeDistanceKm: Double? = null,
    val drivingMode: DrivingMode? = null,
    val warnings: Set<VehicleWarning> = emptySet(),
    val airbagDeployed: Boolean? = null,
    val gear: Gear? = null,
    val brakePedalPressed: Boolean? = null,
    /** Air conditioner (HVAC) running. */
    val hvacOn: Boolean? = null,
    /** Range the Vehicle SDK reports. AAOS may report its own in [CarProperties.rangeRemainingKm]. */
    val rangeRemainingKm: Double? = null,

    // EV
    /** State of charge, 0..100. */
    val batteryPercent: Double? = null,
    val batteryEnergyKwh: Double? = null,
    val batteryCapacityKwh: Double? = null,
    val chargePortOpen: Boolean? = null,
)

/** AAOS standard properties that the Pleos Vehicle SDK does not expose. */
data class CarProperties(
    val speedKmh: Double? = null,
    val ignitionOn: Boolean? = null,
    val rangeRemainingKm: Double? = null,
    val fuelCapacityL: Double? = null,
)

/** Where vehicle data comes from. DEMO is the Fake gateways driven by a scenario. */
enum class DataSourceMode { REAL, DEMO }
