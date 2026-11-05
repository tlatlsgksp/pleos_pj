package com.example.pleos.platform.fake

import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.Capability
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleFeature
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleWarning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

/**
 * In-memory [VehicleGateway] for the demo mode and tests. Setters mutate the snapshot;
 * every change is emitted to collectors of [snapshot].
 */
class FakeVehicleGateway(
    initial: VehicleSnapshot = VehicleSnapshot(),
    private val capabilities: Map<VehicleFeature, Capability> =
        VehicleFeature.entries.associateWith { Capability.READ },
    var fakeVin: String? = "KMHFAKE0000000001",
) : VehicleGateway {

    private val _snapshot = MutableStateFlow(initial)
    override val snapshot: Flow<VehicleSnapshot> = _snapshot.asStateFlow()
    override val activeSource: Flow<DataSourceMode> = flowOf(DataSourceMode.DEMO)

    val current: VehicleSnapshot get() = _snapshot.value

    override suspend fun capabilities(): Map<VehicleFeature, Capability> = capabilities

    override suspend fun vin(): String? = fakeVin

    fun reset(snapshot: VehicleSnapshot) {
        _snapshot.value = snapshot
    }

    fun update(transform: (VehicleSnapshot) -> VehicleSnapshot) = _snapshot.update(transform)

    fun setFuelPercent(percent: Int?) = update { it.copy(fuelPercent = percent) }

    fun setFuelEconomy(instantKmPerL: Double?, currentKmPerL: Double? = instantKmPerL) = update {
        it.copy(instantFuelEconomyKmPerL = instantKmPerL, currentFuelEconomyKmPerL = currentKmPerL)
    }

    fun setTripSinceRefillKm(km: Double?) = update { it.copy(tripSinceRefillKm = km) }

    fun setCumulativeDistanceKm(km: Double?) = update { it.copy(cumulativeDistanceKm = km) }

    fun setDrivingMode(mode: DrivingMode?) = update { it.copy(drivingMode = mode) }

    fun setWarnings(warnings: Set<VehicleWarning>) = update { it.copy(warnings = warnings) }

    fun addWarning(warning: VehicleWarning) = update { it.copy(warnings = it.warnings + warning) }

    fun clearWarning(warning: VehicleWarning) = update { it.copy(warnings = it.warnings - warning) }

    fun setAirbagDeployed(deployed: Boolean?) = update { it.copy(airbagDeployed = deployed) }

    /** Simulates driving: adds distance to the trip counters and burns fuel. */
    fun drive(km: Double, fuelPercentUsed: Int) = update {
        it.copy(
            tripSinceRefillKm = (it.tripSinceRefillKm ?: 0.0) + km,
            cumulativeDistanceKm = (it.cumulativeDistanceKm ?: 0.0) + km,
            fuelPercent = it.fuelPercent?.let { p -> (p - fuelPercentUsed).coerceAtLeast(0) },
        )
    }

    /** Simulates a refuel: the since-refill trip resets, as the real vehicle does. */
    fun refuel(toPercent: Int) = update { it.copy(fuelPercent = toPercent, tripSinceRefillKm = 0.0) }

    fun setGear(gear: Gear?) = update { it.copy(gear = gear) }

    fun setBrakePedalPressed(pressed: Boolean?) = update { it.copy(brakePedalPressed = pressed) }

    fun setHvacOn(on: Boolean?) = update { it.copy(hvacOn = on) }

    fun setRangeRemainingKm(km: Double?) = update { it.copy(rangeRemainingKm = km) }

    fun setChargePortOpen(open: Boolean?) = update { it.copy(chargePortOpen = open) }

    /** Sets battery energy and keeps the state of charge consistent with the capacity. */
    fun setBatteryEnergyKwh(kwh: Double) = update {
        it.copy(
            batteryEnergyKwh = kwh,
            batteryPercent = it.batteryCapacityKwh?.takeIf { cap -> cap > 0 }?.let { cap -> kwh / cap * 100.0 },
        )
    }

    /** EV drive: adds distance and drains the battery. */
    fun driveEv(km: Double, kwhUsed: Double) {
        update { it.copy(cumulativeDistanceKm = (it.cumulativeDistanceKm ?: 0.0) + km) }
        setBatteryEnergyKwh(((current.batteryEnergyKwh ?: 0.0) - kwhUsed).coerceAtLeast(0.0))
    }
}
