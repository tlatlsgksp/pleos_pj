package com.example.pleos.ui.feature.dashboard

import app.cash.turbine.test
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

    private fun viewModel() = DashboardViewModel(
        app.observeDashboard, app.observePendingDraft, app.observeLedger, app.clock, app.chargeStops, app.ecoGuide,
    )

    private fun draft(id: String) = Record(
        id = id, vehicleId = "v1", type = RecordType.ENERGY, occurredAt = app.clock.now, amountWon = 12_976,
        stamp = VehicleStamp(12_370.0, "vin", app.clock.now, StampSource.DEMO),
        origin = RecordOrigin.DETECTED, status = RecordStatus.DRAFT,
        detail = RecordDetail.Energy(EnergyType.EV, 40.0, 324.4),
    )

    @Test
    fun `state follows the EV`() = runTest {
        val vm = viewModel()

        val initial = vm.state.value
        assertEquals(64, initial.batteryPercent)
        assertEquals(310, initial.rangeKm)
        assertEquals(12_345.0, initial.odometerKm!!, 0.0)

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
    fun `the month section opens the fuel and charge tab`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            vm.onEvent(DashboardEvent.OpenLedgerTapped)
            assertEquals(DashboardEffect.NavigateToLedger, awaitItem())
        }
    }

    @Test
    fun `home asks one thing at a time, the charger offer before the draft before warnings`() = runTest {
        val stop = ChargeStopUi("강남역", 8.0, 90.0, 160.0, "대전", isDemoData = true)
        val charge = PendingChargeUi("r1", 40.0, isEv = true, estimatedWon = 12_976)
        val warnings = setOf(VehicleWarning.ENGINE_OIL)

        assertEquals(HomeAction.ChargeStop(stop), DashboardState(chargeStop = stop, pendingCharge = charge, warnings = warnings).action)
        assertEquals(HomeAction.PendingCharge(charge), DashboardState(pendingCharge = charge, warnings = warnings).action)
        assertEquals(HomeAction.Warnings(warnings), DashboardState(warnings = warnings).action)
        assertEquals(HomeAction.Airbag, DashboardState(airbagDeployed = true, chargeStop = stop).action)
        assertNull(DashboardState().action)
    }

    @Test
    fun `this month counts confirmed records only`() = runTest {
        val vm = viewModel()
        app.records.upsert(draft("d1"))
        app.records.upsert(draft("r2").copy(status = RecordStatus.CONFIRMED))

        val month = vm.state.value.month!!
        assertEquals(1, month.energyRecords)
        assertEquals(12_976L, month.spentWon)
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
}
