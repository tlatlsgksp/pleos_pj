package com.example.pleos.ui.feature.charge

import androidx.lifecycle.SavedStateHandle
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
class ChargeConfirmViewModelTest {

    private val app = TestApp()
    private val draft = Record(
        id = "r1", vehicleId = "v1", type = RecordType.ENERGY, occurredAt = app.clock.now, amountWon = 12_976,
        stamp = VehicleStamp(12_370.0, "vin", app.clock.now, StampSource.DEMO),
        origin = RecordOrigin.DETECTED, status = RecordStatus.DRAFT,
        detail = RecordDetail.Energy(EnergyType.EV, 40.0, 324.4, levelBefore = 17.4, levelAfter = 69.1),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: String = "r1") = ChargeConfirmViewModel(
        SavedStateHandle(mapOf(ChargeConfirmViewModel.ARG_RECORD_ID to id)),
        app.records, app.confirmDraft, app.dismissDraft, app.clock,
    )

    @Test
    fun `loads the draft with the estimate as the starting amount`() = runTest {
        app.records.upsert(draft)

        val state = viewModel().state.value

        assertEquals(40.0, state.amountKwh!!, 0.0)
        assertEquals("12976", state.amountInput)
        assertEquals(12_370.0, state.odometerKm!!, 0.0)
        // 03:00 UTC shown in the clock's zone (Asia/Seoul).
        assertEquals(12, state.occurredAt!!.hour)
    }

    @Test
    fun `confirm saves the corrected amount and closes`() = runTest {
        app.records.upsert(draft)
        val vm = viewModel()

        vm.effects.test {
            vm.onEvent(ChargeConfirmEvent.AmountChanged("13,500원"))
            assertEquals("13500", vm.state.value.amountInput)

            vm.onEvent(ChargeConfirmEvent.ConfirmTapped)
            assertEquals(ChargeConfirmEffect.ShowMessage("충전 13,500원을 기록했어요"), awaitItem())
            assertEquals(ChargeConfirmEffect.Close, awaitItem())
        }
        val saved = app.records.get("r1")!!
        assertEquals(RecordStatus.CONFIRMED, saved.status)
        assertEquals(13_500L, saved.amountWon)
    }

    @Test
    fun `dismiss keeps the record out of the ledger`() = runTest {
        app.records.upsert(draft)
        val vm = viewModel()

        vm.effects.test {
            vm.onEvent(ChargeConfirmEvent.DismissTapped)
            awaitItem()
            assertEquals(ChargeConfirmEffect.Close, awaitItem())
        }
        assertEquals(RecordStatus.DISMISSED, app.records.get("r1")!!.status)
    }

    @Test
    fun `an already handled record shows as missing and cannot be saved`() = runTest {
        app.records.upsert(draft.copy(status = RecordStatus.CONFIRMED))
        val vm = viewModel()

        assertTrue(vm.state.value.isMissing)
        vm.effects.test {
            vm.onEvent(ChargeConfirmEvent.ConfirmTapped)
            expectNoEvents()
        }
    }
}
