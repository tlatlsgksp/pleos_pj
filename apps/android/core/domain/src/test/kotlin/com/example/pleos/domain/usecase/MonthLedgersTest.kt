package com.example.pleos.domain.usecase

import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.ExpenseCategory
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.VehicleStamp
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthLedgersTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val now = Instant.parse("2026-11-15T03:00:00Z")

    private fun record(at: String, km: Double, won: Long, detail: RecordDetail, type: RecordType) = Record(
        id = at,
        vehicleId = "v1",
        type = type,
        occurredAt = Instant.parse(at),
        amountWon = won,
        stamp = VehicleStamp(km, null, Instant.parse(at), StampSource.VEHICLE),
        origin = RecordOrigin.DETECTED,
        status = RecordStatus.CONFIRMED,
        detail = detail,
    )

    private fun charge(at: String, km: Double, won: Long, kwh: Double) =
        record(at, km, won, RecordDetail.Energy(EnergyType.EV, kwh), RecordType.ENERGY)

    private val records = listOf(
        charge("2026-10-10T01:00:00Z", 11_600.0, 10_000, 30.0),
        charge("2026-10-28T01:00:00Z", 11_900.0, 12_000, 40.0),
        charge("2026-11-03T01:00:00Z", 12_100.0, 8_000, 25.0),
        record("2026-11-10T01:00:00Z", 12_300.0, 3_900, RecordDetail.Expense(ExpenseCategory.CAR_WASH), RecordType.EXPENSE),
        charge("2026-11-14T01:00:00Z", 12_370.0, 20_000, 60.0),
    )

    @Test
    fun `the current month comes first with distance from the previous month's last reading`() {
        val months = monthLedgers(records, now, zone)
        assertEquals(6, months.size)
        val nov = months[0]
        assertEquals(11, nov.month)
        assertEquals(470.0, nov.distanceKm!!, 0.001)
        assertEquals(85.0, nov.energyAmount, 0.001)
        assertEquals(31_900L, nov.spentWon)
        assertEquals(SpendGroup(RecordType.ENERGY, null, 2, 28_000), nov.groups[0])
        assertEquals(SpendGroup(RecordType.EXPENSE, ExpenseCategory.CAR_WASH, 1, 3_900), nov.groups[1])
    }

    @Test
    fun `the oldest month with data uses its own first reading, an empty month has no distance`() {
        val months = monthLedgers(records, now, zone)
        assertEquals(300.0, months[1].distanceKm!!, 0.001)
        assertNull(months[2].distanceKm)
        assertEquals(0L, months[2].spentWon)
    }
}
