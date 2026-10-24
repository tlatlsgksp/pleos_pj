package com.example.pleos.ui.feature.dashboard

import app.cash.turbine.test
import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.domain.usecase.ObserveDashboardUseCase
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeVehicleGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private class FakeDemoController : DemoScenarioController {
        val playing = MutableStateFlow(false)
        var playCalls = 0
        override val isPlaying: Flow<Boolean> = playing
        override fun play() { playCalls++; playing.value = true }
        override fun stop() { playing.value = false }
    }

    private val vehicle = FakeVehicleGateway(VehicleSnapshot(fuelPercent = 50, currentFuelEconomyKmPerL = 12.0))
    private val car = FakeCarPropertySource(CarProperties(fuelCapacityL = 60.0))
    private val demo = FakeDemoController()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DashboardViewModel(ObserveDashboardUseCase(vehicle, car), demo)

    @Test
    fun `state follows vehicle data and marks the fake source as demo`() = runTest {
        val vm = viewModel()

        val initial = vm.state.value
        assertEquals(50, initial.fuelPercent)
        assertEquals(360, initial.rangeKm)
        assertTrue(initial.isDemo)

        vehicle.setDrivingMode(DrivingMode.ECO)
        vehicle.addWarning(VehicleWarning.ENGINE_OIL)

        val updated = vm.state.value
        assertEquals(DrivingMode.ECO, updated.drivingMode)
        assertEquals(setOf(VehicleWarning.ENGINE_OIL), updated.warnings)
    }

    @Test
    fun `play demo starts the scenario, updates playback state and emits a message`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            vm.onEvent(DashboardEvent.PlayDemoTapped)

            assertEquals(1, demo.playCalls)
            assertTrue(vm.state.value.isDemoPlaying)
            assertEquals(DashboardEffect.ShowMessage("데모 시나리오를 시작했어요"), awaitItem())

            vm.onEvent(DashboardEvent.StopDemoTapped)
            assertEquals(false, vm.state.value.isDemoPlaying)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
