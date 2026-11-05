package com.example.pleos.data.local

import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.DriveSession
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.ExpenseCategory
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.SessionKind
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleStamp
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    private val at = Instant.parse("2026-11-05T03:00:00Z")
    private val stamp = VehicleStamp(12_345.5, "vinhash", at, StampSource.VEHICLE)

    private fun record(type: RecordType, detail: RecordDetail) = Record(
        id = "r-$type", vehicleId = "v1", type = type, occurredAt = at, amountWon = 12_000,
        stamp = stamp, origin = RecordOrigin.DETECTED, status = RecordStatus.DRAFT, detail = detail,
        memo = "memo", rentalPeriodId = null, editedAt = at.plusSeconds(60), integrityHash = "hash",
    )

    @Test
    fun `every record detail survives the round trip`() {
        listOf(
            record(RecordType.ENERGY, RecordDetail.Energy(EnergyType.EV, 30.5, 324.4, "충전소", 25.0, 64.4)),
            record(RecordType.MAINTENANCE, RecordDetail.Maintenance(listOf("cabin_filter", "wiper"), "정비소", 8_000.0)),
            record(RecordType.MAINTENANCE, RecordDetail.Maintenance(emptyList())),
            record(RecordType.EXPENSE, RecordDetail.Expense(ExpenseCategory.CUSTOM, "세차 정기권")),
            record(RecordType.ACCIDENT, RecordDetail.Accident(42.0, 37.5, 127.0, "s1")),
        ).forEach { original ->
            assertEquals(original, original.toEntity().toDomain())
        }
    }

    @Test
    fun `open and closed sessions survive the round trip`() {
        val open = DriveSession("s1", "v1", stamp, null, 0.0, null, 0, null, null, null, SessionKind.MEASURED)
        val closed = open.copy(
            end = stamp.copy(odometerKm = 12_400.0, capturedAt = at.plusSeconds(3_600)),
            distanceKm = 54.5, energyUsed = 9.1, tollWon = 2_400, destinationName = "회사", kind = SessionKind.GAP,
        )

        assertEquals(open, open.toEntity().toDomain())
        assertEquals(closed, closed.toEntity().toDomain())
    }

    @Test
    fun `vehicle and care state survive the round trip`() {
        val vehicle = Vehicle("v1", EnergyType.EV, null, null, 77.4, 12_000.0, at)
        val state = CareItemState("v1", "wiper", null, at, null, 6, "r1")

        assertEquals(vehicle, vehicle.toEntity().toDomain())
        assertEquals(state, state.toEntity().toDomain())
    }
}
