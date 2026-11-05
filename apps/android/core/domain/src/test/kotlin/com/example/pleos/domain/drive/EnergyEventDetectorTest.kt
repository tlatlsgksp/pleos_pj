package com.example.pleos.domain.drive

import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleSnapshot
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyEventDetectorTest {

    private var clock = Instant.parse("2026-11-05T09:00:00Z")
    private fun tick(minutes: Long = 1): Instant = clock.plusSeconds(minutes * 60).also { clock = it }

    private fun ev(kwh: Double, port: Boolean? = false, gear: Gear = Gear.PARK, odo: Double = 12_345.0) = VehicleSnapshot(
        batteryEnergyKwh = kwh,
        batteryCapacityKwh = 77.4,
        batteryPercent = kwh / 77.4 * 100,
        chargePortOpen = port,
        gear = gear,
        cumulativeDistanceKm = odo,
    )

    @Test
    fun `port open, energy rises, port closes gives one charge event`() {
        val d = EnergyEventDetector(EnergyType.EV)

        assertNull(d.onSnapshot(ev(20.0), 0.0, tick()))
        assertNull(d.onSnapshot(ev(20.0, port = true), 0.0, tick()))
        assertTrue(d.isActive)
        assertNull(d.onSnapshot(ev(35.0, port = true), 0.0, tick(30)))
        assertNull(d.onSnapshot(ev(50.0, port = true), 0.0, tick(30)))
        val event = d.onSnapshot(ev(50.0, port = false), 0.0, tick())

        assertNotNull(event)
        assertEquals(30.0, event!!.amount!!, 1e-9)
        assertEquals(20.0 / 77.4 * 100, event.levelBefore!!, 1e-9)
        assertEquals(50.0 / 77.4 * 100, event.levelAfter!!, 1e-9)
        assertEquals(12_345.0, event.odometerKm)
        assertFalse(d.isActive)
    }

    @Test
    fun `energy rising while parked counts even without a charge port signal`() {
        val d = EnergyEventDetector(EnergyType.EV)

        d.onSnapshot(ev(20.0, port = null), 0.0, tick())
        d.onSnapshot(ev(21.0, port = null), 0.0, tick())
        assertTrue(d.isActive)
        d.onSnapshot(ev(30.0, port = null), 0.0, tick(20))
        val event = d.onSnapshot(ev(30.0, port = null, gear = Gear.DRIVE), 0.0, tick())

        // The level before is the sample before the rise, not the first risen one.
        assertEquals(10.0, event!!.amount!!, 1e-9)
    }

    @Test
    fun `increase below 1 kWh is not a charge`() {
        val d = EnergyEventDetector(EnergyType.EV)

        d.onSnapshot(ev(20.0), 0.0, tick())
        d.onSnapshot(ev(20.0, port = true), 0.0, tick())
        d.onSnapshot(ev(20.8, port = true), 0.0, tick(5))

        assertNull(d.onSnapshot(ev(20.8, port = false), 0.0, tick()))
        assertFalse(d.isActive)
    }

    @Test
    fun `shifting to D during a charge ends it`() {
        val d = EnergyEventDetector(EnergyType.EV)

        d.onSnapshot(ev(40.0), 0.0, tick())
        d.onSnapshot(ev(40.0, port = true), 0.0, tick())
        d.onSnapshot(ev(45.0, port = true), 0.0, tick(10))
        val event = d.onSnapshot(ev(45.0, port = true, gear = Gear.DRIVE), 0.0, tick())

        assertEquals(5.0, event!!.amount!!, 1e-9)
    }

    @Test
    fun `regeneration while driving is not a charge`() {
        val d = EnergyEventDetector(EnergyType.EV)

        d.onSnapshot(ev(40.0, gear = Gear.DRIVE), 50.0, tick())
        d.onSnapshot(ev(41.5, gear = Gear.DRIVE), 30.0, tick())

        assertFalse(d.isActive)
    }

    @Test
    fun `fuel refuel converts percent to litres with the tank capacity`() {
        val d = EnergyEventDetector(EnergyType.FUEL, tankCapacityL = 60.0)
        fun fuel(p: Int, gear: Gear = Gear.PARK, trip: Double = 400.0) =
            VehicleSnapshot(fuelPercent = p, gear = gear, tripSinceRefillKm = trip)

        d.onSnapshot(fuel(18), 0.0, tick())
        d.onSnapshot(fuel(40), 0.0, tick())
        d.onSnapshot(fuel(95, trip = 0.0), 0.0, tick(3))
        val event = d.onSnapshot(fuel(95, gear = Gear.DRIVE, trip = 0.0), 0.0, tick())

        assertEquals(EnergyType.FUEL, event!!.kind)
        assertEquals(77.0 / 100 * 60.0, event.amount!!, 1e-9)
    }

    @Test
    fun `fuel rise below 5 percent points is ignored`() {
        val d = EnergyEventDetector(EnergyType.FUEL, tankCapacityL = 60.0)

        d.onSnapshot(VehicleSnapshot(fuelPercent = 50, gear = Gear.PARK), 0.0, tick())
        d.onSnapshot(VehicleSnapshot(fuelPercent = 53, gear = Gear.PARK), 0.0, tick())

        assertNull(d.onSnapshot(VehicleSnapshot(fuelPercent = 53, gear = Gear.DRIVE), 0.0, tick()))
    }
}
