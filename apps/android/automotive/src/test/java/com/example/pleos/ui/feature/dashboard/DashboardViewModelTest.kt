package com.example.pleos.ui.feature.dashboard

import app.cash.turbine.test
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.ReportType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.VehicleStamp
import com.example.pleos.platform.demo.DemoCommunityRepository
import com.example.pleos.ui.testing.TestApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val community = DemoCommunityRepository(app.clock)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DashboardViewModel(
        app.observeDashboard, app.observePendingDraft, app.chargeStops, app.ecoGuide, community, app.navigation, app.settings, app.gateway,
    )

    private fun draft(id: String) = Record(
        id = id, vehicleId = "v1", type = RecordType.ENERGY, occurredAt = app.clock.now, amountWon = 12_976,
        stamp = VehicleStamp(12_370.0, "vin", app.clock.now, StampSource.DEMO),
        origin = RecordOrigin.DETECTED, status = RecordStatus.DRAFT, detail = RecordDetail.Energy(EnergyType.EV, 40.0),
    )

    @Test
    fun `the ride card follows invitation, joining and leaving`() = runTest {
        val vm = viewModel()
        val invited = vm.state.value.rideCard as RideCardUi.Invited
        assertEquals(1, invited.count)

        vm.onEvent(DashboardEvent.JoinRideTapped(invited.riding.id))
        assertTrue(vm.state.value.rideCard is RideCardUi.In)

        vm.onEvent(DashboardEvent.LeaveRideTapped(invited.riding.id))
        // Others are still in the room, so the ride stays open for a rejoin.
        assertTrue(vm.state.value.rideCard is RideCardUi.Left)
    }

    @Test
    fun `a route brings its reports, and the nearest new one drops the banner`() = runTest {
        val vm = viewModel()
        assertTrue(vm.state.value.reportsOnRoute.isEmpty())

        app.navigation.emit(NavEvent.RouteStarted("강릉", 120.0, null))

        assertEquals(2, vm.state.value.reportsOnRoute.size)
        val banner = vm.state.value.banner as HomeBanner.RouteReport
        assertEquals(ReportType.ACCIDENT, banner.report.type)

        vm.onEvent(DashboardEvent.FreshReportExpired)
        assertNull(vm.state.value.banner)
    }

    @Test
    fun `reporting closes the sheet and says what was reported`() = runTest {
        val vm = viewModel()
        vm.onEvent(DashboardEvent.ReportTapped)
        assertTrue(vm.state.value.reportSheetOpen)
        vm.effects.test {
            vm.onEvent(DashboardEvent.ReportPicked(ReportType.DISASTER))
            assertEquals(DashboardEffect.ShowMessage("재난을 제보했어요."), awaitItem())
        }
        assertTrue(!vm.state.value.reportSheetOpen)
    }

    @Test
    fun `the object particle fits the last syllable`() {
        assertEquals("사고를", withObjectParticle("사고"))
        assertEquals("재난을", withObjectParticle("재난"))
        assertEquals("단속카메라를", withObjectParticle("단속카메라"))
    }
}
