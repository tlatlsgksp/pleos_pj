package com.example.pleos.domain.usecase

import app.cash.turbine.test
import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.Capability
import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.VehicleFeature
import com.example.pleos.domain.model.VehicleSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveDashboardUseCaseTest {

    private class TestVehicleGateway(initial: VehicleSnapshot) : VehicleGateway {
        val state = MutableStateFlow(initial)
        override val snapshot: Flow<VehicleSnapshot> = state
        override val activeSource: Flow<DataSourceMode> = flowOf(DataSourceMode.DEMO)
        override suspend fun capabilities(): Map<VehicleFeature, Capability> = emptyMap()
        override suspend fun vin(): String? = null
    }

    private class TestCarPropertySource(initial: CarProperties) : CarPropertySource {
        val state = MutableStateFlow(initial)
        override val properties: Flow<CarProperties> = state
    }

    @Test
    fun `prefers vehicle-reported range over the estimate`() = runTest {
        val vehicle = TestVehicleGateway(VehicleSnapshot(fuelPercent = 50, currentFuelEconomyKmPerL = 12.0))
        val car = TestCarPropertySource(CarProperties(rangeRemainingKm = 410.0, fuelCapacityL = 60.0))

        ObserveDashboardUseCase(vehicle, car)().test {
            val data = awaitItem()
            assertEquals(410.0, data.estimatedRangeKm)
            assertEquals(DataSourceMode.DEMO, data.source)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `estimates range from fuel percent, tank capacity and economy`() = runTest {
        val vehicle = TestVehicleGateway(VehicleSnapshot(fuelPercent = 50, currentFuelEconomyKmPerL = 12.0))
        val car = TestCarPropertySource(CarProperties(fuelCapacityL = 60.0))

        ObserveDashboardUseCase(vehicle, car)().test {
            assertEquals(360.0, awaitItem().estimatedRangeKm)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `range is null when an input is missing and re-emits on vehicle change`() = runTest {
        val vehicle = TestVehicleGateway(VehicleSnapshot(fuelPercent = 50))
        val car = TestCarPropertySource(CarProperties(fuelCapacityL = 60.0))

        ObserveDashboardUseCase(vehicle, car)().test {
            assertNull(awaitItem().estimatedRangeKm)
            vehicle.state.value = vehicle.state.value.copy(currentFuelEconomyKmPerL = 10.0)
            assertEquals(300.0, awaitItem().estimatedRangeKm)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
