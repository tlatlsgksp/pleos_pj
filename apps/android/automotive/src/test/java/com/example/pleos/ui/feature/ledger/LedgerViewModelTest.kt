package com.example.pleos.ui.feature.ledger

import app.cash.turbine.test
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.VehicleStamp
import com.example.pleos.ui.testing.TestApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LedgerViewModelTest {

    private val app = TestApp()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun charge(id: String, status: RecordStatus) = Record(
        id = id, vehicleId = "v1", type = RecordType.ENERGY, occurredAt = app.clock.now, amountWon = 12_976,
        stamp = VehicleStamp(12_370.0, "vin", app.clock.now, StampSource.DEMO),
        origin = RecordOrigin.DETECTED, status = status,
        detail = RecordDetail.Energy(EnergyType.EV, 40.0, 324.4, levelBefore = 23.0, levelAfter = 75.0),
    )

    @Test
    fun `lists fuel and charge records without maintenance, and filters by type`() = runTest {
        app.records.upsert(charge("d1", RecordStatus.DRAFT))
        app.records.upsert(charge("c1", RecordStatus.CONFIRMED))
        app.recordMaintenance(listOf("cabin_filter"))
        val vm = LedgerViewModel(app.observeLedger, app.confirmDraft, app.dismissDraft, app.clock)

        val rows = vm.state.value.items
        assertEquals(2, rows.size)
        val draft = rows.first { it.id == "d1" }
        assertTrue(draft.isDraft)
        assertEquals("충전 40.0 kWh", draft.title)
        assertEquals("11월 5일 12:00", draft.detail)
        assertEquals(23.0, draft.levelBefore!!, 0.0)
        assertEquals(12_370.0, draft.odometerKm!!, 0.0)
        assertTrue(rows.none { it.type == RecordType.MAINTENANCE })

        vm.onEvent(LedgerEvent.FilterSelected(RecordFilter.EXPENSE))
        assertTrue(vm.state.value.items.isEmpty())
        vm.onEvent(LedgerEvent.FilterSelected(RecordFilter.ENERGY))
        assertEquals(2, vm.state.value.items.size)
        assertEquals(11, vm.state.value.months.first().month)
    }

    @Test
    fun `tapping a draft fills the amount field, and saving confirms it with the typed amount`() = runTest {
        app.records.upsert(charge("d1", RecordStatus.DRAFT))
        app.records.upsert(charge("c1", RecordStatus.CONFIRMED))
        val vm = LedgerViewModel(app.observeLedger, app.confirmDraft, app.dismissDraft, app.clock)

        vm.onEvent(LedgerEvent.RecordTapped("c1"))
        val detail = vm.state.value.selectedRecord!!
        assertEquals("c1", detail.id)
        assertTrue(detail.facts.any { it == ("주행거리" to "12,370 km") })

        vm.onEvent(LedgerEvent.RecordTapped("d1"))
        assertEquals("12976", vm.state.value.amountInput)
        vm.onEvent(LedgerEvent.AmountChanged("13000"))
        vm.effects.test {
            vm.onEvent(LedgerEvent.SaveDraftTapped("c1"))
            vm.onEvent(LedgerEvent.SaveDraftTapped("d1"))
            assertEquals(LedgerEffect.ShowMessage("기록을 저장했어요"), awaitItem())
        }
        val saved = app.records.get("d1")!!
        assertEquals(RecordStatus.CONFIRMED, saved.status)
        assertEquals(13_000L, saved.amountWon)
        assertEquals(null, vm.state.value.selection)

        vm.onEvent(LedgerEvent.TabSelected(LedgerTab.STATS))
        assertEquals(null, vm.state.value.selection)
    }
}
