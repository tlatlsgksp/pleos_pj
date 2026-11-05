package com.example.pleos.ui.feature.dashboard

import app.cash.turbine.test
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.WaypointSlot
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.VehicleStamp
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.ui.testing.TestApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val app = TestApp()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DashboardViewModel(app.observeDashboard, app.observePendingDraft, app.observeCare, app.demo, app.settings, app.chargeStops)

    private fun draft(id: String) = Record(
        id = id, vehicleId = "v1", type = RecordType.ENERGY, occurredAt = app.clock.now, amountWon = 12_976,
        stamp = VehicleStamp(12_370.0, "vin", app.clock.now, StampSource.DEMO),
        origin = RecordOrigin.DETECTED, status = RecordStatus.DRAFT,
        detail = RecordDetail.Energy(EnergyType.EV, 40.0, 324.4),
    )

    @Test
    fun `state follows the EV and marks the fake source as demo`() = runTest {
        val vm = viewModel()

        val initial = vm.state.value
        assertEquals(64, initial.batteryPercent)
        assertEquals(310, initial.rangeKm)
        assertEquals(12_345.0, initial.odometerKm!!, 0.0)
        assertTrue(initial.isDemo)

        app.gateway.update { it.copy(warnings = setOf(VehicleWarning.ENGINE_OIL), cumulativeDistanceKm = 12_400.0) }

        assertEquals(setOf(VehicleWarning.ENGINE_OIL), vm.state.value.warnings)
        assertEquals(12_400.0, vm.state.value.odometerKm!!, 0.0)
    }

    @Test
    fun `a new charge draft shows a card and a message, and opens the confirm screen`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            app.records.upsert(draft("r1"))
            assertEquals(PendingChargeUi("r1", 40.0, isEv = true, estimatedWon = 12_976), vm.state.value.pendingCharge)
            assertTrue(awaitItem() is DashboardEffect.ShowMessage)

            vm.onEvent(DashboardEvent.OpenChargeTapped)
            assertEquals(DashboardEffect.NavigateToCharge("r1"), awaitItem())

            app.records.upsert(draft("r1").copy(status = RecordStatus.CONFIRMED))
            assertNull(vm.state.value.pendingCharge)
            expectNoEvents()
        }
    }

    @Test
    fun `care preview shows at most three items, most urgent first`() = runTest {
        app.recordMaintenance(listOf("wiper"))
        val vm = viewModel()

        assertEquals(DashboardState.CARE_PREVIEW_COUNT, vm.state.value.careItems.size)
        vm.effects.test {
            vm.onEvent(DashboardEvent.OpenCareTapped)
            assertEquals(DashboardEffect.NavigateToCare, awaitItem())
        }
    }

    @Test
    fun `the source toggle asks for the real vehicle from the demo and back`() = runTest {
        val vm = viewModel()
        app.settings.dataSourceMode.test {
            assertEquals(DataSourceMode.DEMO, awaitItem())

            vm.onEvent(DashboardEvent.SourceToggleTapped)
            assertEquals(DataSourceMode.REAL, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `low range on a long route shows a charger card that adds the first waypoint`() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { app.chargeStops.run() }
        val vm = viewModel()
        app.gateway.update { it.copy(rangeRemainingKm = 90.0) }

        vm.effects.test {
            app.navigation.emit(NavEvent.RouteStarted("대전", 160.0, null))
            val stop = vm.state.value.chargeStop!!
            assertEquals("시연용 충전소 · 강남역", stop.name)
            assertTrue(stop.isDemoData)

            vm.onEvent(DashboardEvent.AddChargeStopTapped)
            assertEquals(DashboardEffect.ShowMessage("충전소를 내비 경유지로 넣었어요"), awaitItem())
        }
        assertEquals(WaypointSlot.FIRST, app.navigation.waypoints.single().second)
        assertNull(vm.state.value.chargeStop)
    }

    @Test
    fun `play demo starts the scenario, updates playback state and emits a message`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            vm.onEvent(DashboardEvent.PlayDemoTapped)

            assertEquals(1, app.demo.playCalls)
            assertTrue(vm.state.value.isDemoPlaying)
            assertEquals(DashboardEffect.ShowMessage("데모 시나리오를 시작했어요"), awaitItem())

            vm.onEvent(DashboardEvent.StopDemoTapped)
            assertEquals(false, vm.state.value.isDemoPlaying)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
