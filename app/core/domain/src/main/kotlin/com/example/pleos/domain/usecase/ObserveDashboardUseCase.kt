package com.example.pleos.domain.usecase

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.VehicleSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDashboardUseCase @Inject constructor(
    private val vehicle: VehicleGateway,
    private val car: CarPropertySource,
) {
    operator fun invoke(): Flow<DashboardData> =
        combine(vehicle.snapshot, car.properties, vehicle.activeSource) { snapshot, properties, source ->
            DashboardData(
                vehicle = snapshot,
                car = properties,
                estimatedRangeKm = estimateRangeKm(snapshot, properties),
                source = source,
            )
        }
}

/** AAOS range if the vehicle reports it; otherwise fuel % × tank capacity × current economy; null if any input is missing. */
fun estimateRangeKm(snapshot: VehicleSnapshot, properties: CarProperties): Double? {
    properties.rangeRemainingKm?.let { return it }
    val percent = snapshot.fuelPercent ?: return null
    val capacityL = properties.fuelCapacityL ?: return null
    val economy = snapshot.currentFuelEconomyKmPerL ?: return null
    return percent / 100.0 * capacityL * economy
}
