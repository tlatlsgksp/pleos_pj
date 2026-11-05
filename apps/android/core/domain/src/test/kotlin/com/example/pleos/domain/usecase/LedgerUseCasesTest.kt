package com.example.pleos.domain.usecase

import app.cash.turbine.test
import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleStamp
import com.example.pleos.domain.testing.InMemoryCareRepository
import com.example.pleos.domain.testing.InMemoryDriveSessionRepository
import com.example.pleos.domain.testing.InMemoryRecordRepository
import com.example.pleos.domain.testing.InMemoryVehicleRepository
import com.example.pleos.domain.testing.MutableClock
import com.example.pleos.domain.testing.MutableVehicleGateway
import com.example.pleos.domain.testing.SequentialIds
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerUseCasesTest {

    private val clock = MutableClock(Instant.parse("2026-11-05T03:00:00Z"))
    private val vehicle = Vehicle("v1", EnergyType.EV, "vin", null, 77.4, 12_000.0, clock.now)
    private val gateway = MutableVehicleGateway(VehicleSnapshot(cumulativeDistanceKm = 12_345.0))
    private val vehicles = InMemoryVehicleRepository(vehicle)
    private val records = InMemoryRecordRepository()
    private val care = InMemoryCareRepository()
    private val sessions = InMemoryDriveSessionRepository()
    private val stamp = StampVehicleUseCase(gateway, sessions, clock)
    private val recordMaintenance = RecordMaintenanceUseCase(vehicles, records, care, stamp, SequentialIds(), clock)
    private val observeCare = ObserveCareItemsUseCase(vehicles, care, gateway, clock)

    @Test
    fun `service now, then the odometer rises and the item moves to due soon`() = runTest {
        recordMaintenance(listOf("cabin_filter"))

        observeCare().test {
            val first = awaitItem().first { it.def.id == "cabin_filter" }
            assertEquals(CareStatus.OK, first.progress.status)
            assertEquals(0.0, first.progress.ratio!!, 1e-9)

            gateway.update { it.copy(cumulativeDistanceKm = 12_345.0 + 12_500.0) }
            val after = awaitItem().first { it.def.id == "cabin_filter" }
            assertEquals(CareStatus.DUE_SOON, after.progress.status)
            assertEquals(2_500.0, after.progress.remainingKm!!, 1e-9)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `items of the other energy type are not shown and urgent items come first`() = runTest {
        care.replaceAll("v1", listOf(CareItemState("v1", "tire_rotation", lastServiceOdometerKm = 0.0, lastServiceAt = clock.now)))

        observeCare().test {
            val items = awaitItem()
            assertTrue(items.none { it.def.id == "engine_oil" })
            assertEquals("tire_rotation", items.first().def.id)
            assertEquals(CareStatus.OVERDUE, items.first().progress.status)
            assertEquals(CareStatus.UNKNOWN, items.last().progress.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maintenance record carries the live odometer stamp`() = runTest {
        val record = recordMaintenance(listOf("wiper"), amountWon = 20_000)

        assertEquals(RecordType.MAINTENANCE, record.type)
        assertEquals(RecordStatus.CONFIRMED, record.status)
        assertEquals(12_345.0, record.stamp.odometerKm, 0.0)
        assertEquals(StampSource.DEMO, record.stamp.source)
        assertEquals("vin", record.stamp.vinHash)
    }

    @Test
    fun `stamp falls back to the baseline and says MANUAL without a live odometer`() = runTest {
        gateway.state.value = VehicleSnapshot()

        val record = recordMaintenance(listOf("wiper"))

        assertEquals(StampSource.MANUAL, record.stamp.source)
        assertEquals(12_000.0, record.stamp.odometerKm, 0.0)
    }

    @Test
    fun `a past service entered later does not overwrite a newer one`() = runTest {
        val newer = recordMaintenance(listOf("cabin_filter"))
        recordMaintenance(
            listOf("cabin_filter"),
            occurredAt = Instant.parse("2026-03-01T00:00:00Z"),
            serviceOdometerKm = 8_000.0,
        )

        val state = care.states("v1").single { it.itemId == "cabin_filter" }
        assertEquals(newer.id, state.lastRecordId)
        assertEquals(12_345.0, state.lastServiceOdometerKm!!, 0.0)
    }

    @Test
    fun `deleting the latest service falls back to the previous one`() = runTest {
        val older = recordMaintenance(
            listOf("cabin_filter"),
            occurredAt = Instant.parse("2026-03-01T00:00:00Z"),
            serviceOdometerKm = 8_000.0,
        )
        val newer = recordMaintenance(listOf("cabin_filter"))

        DeleteRecordUseCase(records, care)(newer.id)

        val state = care.states("v1").single { it.itemId == "cabin_filter" }
        assertEquals(older.id, state.lastRecordId)
        assertEquals(8_000.0, state.lastServiceOdometerKm!!, 0.0)
    }

    @Test
    fun `deleting the only service keeps the custom interval but clears the service`() = runTest {
        care.replaceAll("v1", listOf(CareItemState("v1", "wiper", customIntervalMonths = 6)))
        val only = recordMaintenance(listOf("wiper"))

        DeleteRecordUseCase(records, care)(only.id)

        val state = care.states("v1").single()
        assertNull(state.lastServiceAt)
        assertEquals(6, state.customIntervalMonths)
    }

    @Test
    fun `pending draft is confirmed with the amount the user entered, or dismissed`() = runTest {
        val draft = Record(
            id = "d1", vehicleId = "v1", type = RecordType.ENERGY, occurredAt = clock.now, amountWon = 9_000,
            stamp = VehicleStamp(12_345.0, "vin", clock.now, StampSource.DEMO),
            origin = RecordOrigin.DETECTED, status = RecordStatus.DRAFT,
            detail = RecordDetail.Energy(EnergyType.EV, 30.0),
        )
        records.upsert(draft)
        records.upsert(draft.copy(id = "d2"))

        ObservePendingDraftUseCase(vehicles, records)().test {
            assertEquals(RecordStatus.DRAFT, awaitItem()!!.status)

            assertTrue(ConfirmDraftUseCase(records)("d1", 9_500))
            assertEquals("d2", awaitItem()!!.id)

            DismissDraftUseCase(records)("d2")
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(9_500L, records.get("d1")!!.amountWon)
        assertEquals(RecordStatus.DISMISSED, records.get("d2")!!.status)
        assertFalse(ConfirmDraftUseCase(records)("d2", 1))
    }
}
