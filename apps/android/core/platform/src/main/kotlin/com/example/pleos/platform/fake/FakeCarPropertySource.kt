package com.example.pleos.platform.fake

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.model.CarProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [CarPropertySource] for the demo mode and tests. */
class FakeCarPropertySource(
    initial: CarProperties = CarProperties(),
) : CarPropertySource {

    private val _properties = MutableStateFlow(initial)
    override val properties: Flow<CarProperties> = _properties.asStateFlow()

    val current: CarProperties get() = _properties.value

    fun reset(properties: CarProperties) {
        _properties.value = properties
    }

    fun update(transform: (CarProperties) -> CarProperties) = _properties.update(transform)

    fun setSpeedKmh(speed: Double?) = update { it.copy(speedKmh = speed) }

    fun setIgnitionOn(on: Boolean?) = update { it.copy(ignitionOn = on) }

    fun setRangeRemainingKm(km: Double?) = update { it.copy(rangeRemainingKm = km) }

    fun setFuelCapacityL(liters: Double?) = update { it.copy(fuelCapacityL = liters) }
}
