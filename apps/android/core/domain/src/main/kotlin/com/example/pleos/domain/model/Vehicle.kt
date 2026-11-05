package com.example.pleos.domain.model

import java.time.Instant

/** Hybrids start as FUEL (FEATURE_LOGIC 6-1, undecided). */
enum class EnergyType { FUEL, EV }

/** The car this app keeps the ledger for. The id is app-generated; the VIN is never used as a key. */
data class Vehicle(
    val id: String,
    val energyType: EnergyType,
    /** SHA-256 of the VIN. The raw VIN never leaves the gateway layer. */
    val vinHash: String?,
    /** FUEL. Vehicle value first, then user input. */
    val tankCapacityL: Double?,
    /** EV. Vehicle value (emulator: 77.4 kWh). */
    val batteryCapacityKwh: Double?,
    /** Total odometer when the app first started on this car. */
    val baselineOdometerKm: Double,
    val baselineAt: Instant,
)
