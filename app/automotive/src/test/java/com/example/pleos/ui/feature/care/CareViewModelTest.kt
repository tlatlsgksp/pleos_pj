package com.example.pleos.ui.feature.care

import app.cash.turbine.test
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.model.RecordType
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
class CareViewModelTest {

    private val app = TestApp()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists only EV items and starts them all as unknown`() = runTest {
        val vm = CareViewModel(app.observeCare, app.recordMaintenance)

        val items = vm.state.value.items
        assertTrue(items.isNotEmpty())
        assertTrue(items.none { it.id == "engine_oil" })
        assertTrue(items.all { it.status == CareStatus.UNKNOWN })
    }

    @Test
    fun `serviced today saves a maintenance record, then the odometer moves the progress`() = runTest {
        val vm = CareViewModel(app.observeCare, app.recordMaintenance)

        vm.effects.test {
            vm.onEvent(CareEvent.ServicedTapped("cabin_filter"))
            assertEquals(CareEffect.ShowMessage("에어컨 필터 교체를 기록했어요"), awaitItem())
        }

        val record = app.records.all.value.values.single()
        assertEquals(RecordType.MAINTENANCE, record.type)
        assertNull(vm.state.value.savingItemId)
        val filter = vm.state.value.items.first()
        assertEquals("cabin_filter", filter.id)
        assertEquals(CareStatus.OK, filter.status)

        app.gateway.update { it.copy(cumulativeDistanceKm = 12_345.0 + 16_000.0) }

        val moved = vm.state.value.items.first { it.id == "cabin_filter" }
        assertEquals(CareStatus.OVERDUE, moved.status)
        assertEquals(-1_000.0, moved.remainingKm!!, 1e-9)
    }

    @Test
    fun `back navigates back`() = runTest {
        val vm = CareViewModel(app.observeCare, app.recordMaintenance)

        vm.effects.test {
            vm.onEvent(CareEvent.BackTapped)
            assertEquals(CareEffect.NavigateBack, awaitItem())
        }
    }
}
