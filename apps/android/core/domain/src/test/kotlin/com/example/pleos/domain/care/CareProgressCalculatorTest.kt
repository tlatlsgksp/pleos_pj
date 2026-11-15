package com.example.pleos.domain.care

import com.example.pleos.domain.model.CareCatalog
import com.example.pleos.domain.model.CareItemDef
import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.model.EnergyType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareProgressCalculatorTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private fun at(date: String) = LocalDate.parse(date).atStartOfDay(zone).plusHours(12).toInstant()

    private val both = CareItemDef("x", "x", setOf(EnergyType.EV), intervalKm = 10_000, intervalMonths = 12, source = "test")

    @Test
    fun `Jan 31 to Feb 28 is one calendar month and matches the due date`() {
        val from = LocalDate.parse("2026-01-31")
        assertEquals(1.0, CareProgressCalculator.calendarMonthsBetween(from, LocalDate.parse("2026-02-28")), 1e-9)
        assertEquals(LocalDate.parse("2026-02-28"), from.plusMonths(1))

        val oneMonth = CareItemDef("m", "m", setOf(EnergyType.EV), intervalKm = null, intervalMonths = 1, source = "test")
        val state = CareItemState("v", "m", lastServiceAt = at("2026-01-31"))

        val dayBefore = CareProgressCalculator.progress(oneMonth, state, null, at("2026-02-27"), zone)
        val onDue = CareProgressCalculator.progress(oneMonth, state, null, at("2026-02-28"), zone)

        assertEquals(CareStatus.DUE_SOON, dayBefore.status)
        assertEquals(CareStatus.OVERDUE, onDue.status)
        assertEquals(LocalDate.parse("2026-02-28"), onDue.dueDate)
        assertEquals(1, onDue.elapsedMonths)
    }

    @Test
    fun `partial month is a fraction of that month`() {
        val months = CareProgressCalculator.calendarMonthsBetween(LocalDate.parse("2026-03-15"), LocalDate.parse("2026-04-30"))
        // Mar 15 -> Apr 15 is one month, Apr 15 -> Apr 30 is 15 of the 30 days to May 15.
        assertEquals(1.5, months, 1e-9)
    }

    @Test
    fun `progress is the larger of distance and time`() {
        val state = CareItemState("v", "x", lastServiceOdometerKm = 20_000.0, lastServiceAt = at("2026-01-01"))

        val p = CareProgressCalculator.progress(both, state, currentOdometerKm = 29_000.0, now = at("2026-04-01"), zone = zone)

        assertEquals(0.9, p.distanceRatio!!, 1e-9)
        assertEquals(0.25, p.monthsRatio!!, 1e-9)
        assertEquals(0.9, p.ratio!!, 1e-9)
        assertEquals(CareStatus.DUE_SOON, p.status)
        assertEquals(1_000.0, p.remainingKm!!, 1e-9)
    }

    @Test
    fun `odometer rising moves the progress, which is flow 1 of the demo`() {
        val state = CareItemState("v", "x", lastServiceOdometerKm = 20_000.0, lastServiceAt = at("2026-10-01"))
        val now = at("2026-11-05")

        val before = CareProgressCalculator.progress(both, state, 27_000.0, now, zone)
        val after = CareProgressCalculator.progress(both, state, 30_500.0, now, zone)

        assertEquals(CareStatus.OK, before.status)
        assertEquals(CareStatus.OVERDUE, after.status)
    }

    @Test
    fun `item with only a distance interval uses distance only`() {
        val pad = CareCatalog.get("brake_pad_front")!!
        val state = CareItemState("v", pad.id, lastServiceOdometerKm = 0.0, lastServiceAt = at("2020-01-01"))

        val p = CareProgressCalculator.progress(pad, state, 15_000.0, at("2026-11-05"), zone)

        assertNull(p.monthsRatio)
        assertEquals(0.5, p.ratio!!, 1e-9)
        assertEquals(CareStatus.OK, p.status)
    }

    @Test
    fun `item with only a time interval uses time only`() {
        val wiper = CareCatalog.get("wiper")!!
        val state = CareItemState("v", wiper.id, lastServiceAt = at("2025-11-05"))

        val p = CareProgressCalculator.progress(wiper, state, 99_999.0, at("2026-11-05"), zone)

        assertNull(p.distanceRatio)
        assertEquals(1.0, p.ratio!!, 1e-9)
        assertEquals(CareStatus.OVERDUE, p.status)
    }

    @Test
    fun `no last service gives UNKNOWN instead of a guess`() {
        val p = CareProgressCalculator.progress(both, state = null, currentOdometerKm = 50_000.0, now = at("2026-11-05"), zone = zone)

        assertEquals(CareStatus.UNKNOWN, p.status)
        assertNull(p.ratio)
    }

    @Test
    fun `custom interval overrides the catalog`() {
        val state = CareItemState("v", "x", lastServiceOdometerKm = 0.0, customIntervalKm = 5_000)

        val p = CareProgressCalculator.progress(both, state, 4_500.0, at("2026-11-05"), zone)

        assertEquals(0.9, p.ratio!!, 1e-9)
    }

    @Test
    fun `catalog splits items by energy type`() {
        val ev = CareCatalog.forEnergyType(EnergyType.EV).map { it.id }
        val fuel = CareCatalog.forEnergyType(EnergyType.FUEL).map { it.id }

        assertTrue("reducer_oil" in ev && "engine_oil" !in ev)
        assertTrue("engine_oil" in fuel && "reducer_oil" !in fuel)
        assertTrue("cabin_filter" in ev && "cabin_filter" in fuel)
    }
}
