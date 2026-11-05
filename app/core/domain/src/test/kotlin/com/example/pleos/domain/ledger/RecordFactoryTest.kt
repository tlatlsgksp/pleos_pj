package com.example.pleos.domain.ledger

import com.example.pleos.domain.drive.EnergyEvent
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordFactoryTest {

    private val now = Instant.parse("2026-11-05T10:00:00Z")
    private val vehicle = Vehicle("v1", EnergyType.EV, "hash", null, 77.4, 12_000.0, now)
    private val event = EnergyEvent(EnergyType.EV, now.minusSeconds(3_600), now, 30.0, 25.0, 64.0, 12_345.0)

    @Test
    fun `stamp uses the live odometer, or falls back and says MANUAL`() {
        val live = RecordFactory.stamp(12_345.0, "hash", now, StampSource.VEHICLE, fallbackOdometerKm = 12_000.0)
        val fallback = RecordFactory.stamp(null, "hash", now, StampSource.VEHICLE, fallbackOdometerKm = 12_000.0)

        assertEquals(StampSource.VEHICLE, live.source)
        assertEquals(12_345.0, live.odometerKm, 0.0)
        assertEquals(StampSource.MANUAL, fallback.source)
        assertEquals(12_000.0, fallback.odometerKm, 0.0)
    }

    @Test
    fun `detected charge becomes a DRAFT energy record with a stamp and a price estimate`() {
        val stamp = RecordFactory.stamp(12_345.0, "hash", now, StampSource.VEHICLE, 12_000.0)

        val draft = RecordFactory.energyDraft("r1", vehicle, event, stamp, unitPriceWon = 347.2)

        assertEquals(RecordType.ENERGY, draft.type)
        assertEquals(RecordStatus.DRAFT, draft.status)
        assertEquals(RecordOrigin.DETECTED, draft.origin)
        assertEquals(10_416L, draft.amountWon)
        assertEquals(12_345.0, draft.stamp.odometerKm, 0.0)
        assertEquals(30.0, (draft.detail as RecordDetail.Energy).amount!!, 0.0)
    }

    @Test
    fun `without a unit price the amount is left for the user`() {
        val stamp = RecordFactory.stamp(12_345.0, "hash", now, StampSource.VEHICLE, 12_000.0)

        assertNull(RecordFactory.energyDraft("r1", vehicle, event, stamp).amountWon)
    }

    @Test
    fun `confirming keeps the stamp, stores the amount and rehashes`() {
        val stamp = RecordFactory.stamp(12_345.0, "hash", now, StampSource.VEHICLE, 12_000.0)
        val draft = RecordFactory.energyDraft("r1", vehicle, event, stamp, unitPriceWon = 347.2)

        val confirmed = RecordFactory.confirm(draft, amountWon = 11_000L)

        assertEquals(RecordStatus.CONFIRMED, confirmed.status)
        assertEquals(11_000L, confirmed.amountWon)
        assertEquals(draft.stamp, confirmed.stamp)
        assertNull(confirmed.editedAt)
        assertNotEquals(draft.integrityHash, confirmed.integrityHash)
        assertEquals(RecordFactory.integrityHash(confirmed), confirmed.integrityHash)
    }
}
