package com.example.pleos.domain.drive

import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleSnapshot
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSessionTrackerTest {

    private var clock = Instant.parse("2026-11-05T08:00:00Z")
    private fun tick(seconds: Long = 60): Instant = clock.plusSeconds(seconds).also { clock = it }
    private fun s(gear: Gear, odo: Double) = VehicleSnapshot(gear = gear, cumulativeDistanceKm = odo)

    @Test
    fun `P to D starts, three minutes in P ends, distance and energy come from the odometer and battery`() {
        val t = DriveSessionTracker(lastEndOdometerKm = 1_000.0)

        assertTrue(t.onSnapshot(s(Gear.PARK, 1_000.0), 60.0, tick()).isEmpty())
        val started = t.onSnapshot(s(Gear.DRIVE, 1_000.0), 60.0, tick()).single() as SessionEvent.Started
        assertNull(started.gapKm)

        t.onSnapshot(s(Gear.DRIVE, 1_012.0), 58.0, tick(900))
        t.onSnapshot(s(Gear.PARK, 1_024.0), 56.0, tick(900))
        assertTrue(t.onSnapshot(s(Gear.PARK, 1_024.0), 56.0, tick(120)).isEmpty())
        val ended = t.onSnapshot(s(Gear.PARK, 1_024.0), 56.0, tick(60)).single() as SessionEvent.Ended

        assertEquals(24.0, ended.distanceKm, 1e-9)
        assertEquals(4.0, ended.energyUsed!!, 1e-9)
        assertEquals(SessionEvent.EndReason.PARKED, ended.reason)
        assertFalse(t.isOpen)
    }

    @Test
    fun `a short stop in P does not split the drive`() {
        val t = DriveSessionTracker(lastEndOdometerKm = 0.0)
        t.onSnapshot(s(Gear.PARK, 0.0), null, tick())
        t.onSnapshot(s(Gear.DRIVE, 0.0), null, tick())
        t.onSnapshot(s(Gear.PARK, 5.0), null, tick())
        t.onSnapshot(s(Gear.PARK, 5.0), null, tick(120))
        val events = t.onSnapshot(s(Gear.DRIVE, 5.0), null, tick(30))

        assertTrue(events.isEmpty())
        assertTrue(t.isOpen)
    }

    @Test
    fun `odometer rising starts a session even without a gear change`() {
        val t = DriveSessionTracker(lastEndOdometerKm = 500.0)
        t.onSnapshot(VehicleSnapshot(cumulativeDistanceKm = 500.0), null, tick())
        val started = t.onSnapshot(VehicleSnapshot(cumulativeDistanceKm = 501.0), null, tick()).single() as SessionEvent.Started

        assertEquals(500.0, started.odometerKm)
    }

    @Test
    fun `distance driven while the app was off becomes a gap`() {
        val t = DriveSessionTracker(lastEndOdometerKm = 1_000.0, lastEndEnergy = 60.0)
        t.onSnapshot(s(Gear.PARK, 1_042.0), 52.0, tick())
        val started = t.onSnapshot(s(Gear.DRIVE, 1_042.0), 52.0, tick()).single() as SessionEvent.Started

        assertEquals(42.0, started.gapKm!!, 1e-9)
        assertEquals(8.0, started.gapEnergy!!, 1e-9)
    }

    @Test
    fun `gap below half a kilometre is ignored`() {
        val t = DriveSessionTracker(lastEndOdometerKm = 1_000.0)
        t.onSnapshot(s(Gear.PARK, 1_000.4), null, tick())
        val started = t.onSnapshot(s(Gear.DRIVE, 1_000.4), null, tick()).single() as SessionEvent.Started

        assertNull(started.gapKm)
    }

    @Test
    fun `a charge ends the open drive`() {
        val t = DriveSessionTracker(lastEndOdometerKm = 0.0)
        t.onSnapshot(s(Gear.PARK, 0.0), 30.0, tick())
        t.onSnapshot(s(Gear.DRIVE, 0.0), 30.0, tick())
        t.onSnapshot(s(Gear.PARK, 10.0), 28.0, tick())

        val ended = t.onEnergyEventStarted(tick()) as SessionEvent.Ended

        assertEquals(10.0, ended.distanceKm, 1e-9)
        assertEquals(SessionEvent.EndReason.ENERGY_EVENT, ended.reason)
        assertNull(t.onEnergyEventStarted(tick()))
    }
}
