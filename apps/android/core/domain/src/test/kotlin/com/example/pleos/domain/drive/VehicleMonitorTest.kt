package com.example.pleos.domain.drive

import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.SessionKind
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.testing.InMemoryDriveSessionRepository
import com.example.pleos.domain.testing.InMemoryRecordRepository
import com.example.pleos.domain.testing.InMemorySettingsRepository
import com.example.pleos.domain.testing.InMemoryVehicleRepository
import com.example.pleos.domain.testing.MutableCarPropertySource
import com.example.pleos.domain.testing.MutableClock
import com.example.pleos.domain.testing.MutableVehicleGateway
import com.example.pleos.domain.testing.SequentialIds
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleMonitorTest {

    private val clock = MutableClock()
    private val gateway = MutableVehicleGateway()
    private val vehicles = InMemoryVehicleRepository()
    private val sessions = InMemoryDriveSessionRepository()
    private val records = InMemoryRecordRepository()
    private val ids = SequentialIds()
    private fun newMonitor() = VehicleMonitor(
        gateway, MutableCarPropertySource(), vehicles, sessions, records,
        InMemorySettingsRepository(price = 300.0), ids, clock,
    )
    private var monitor = newMonitor()
    private val car = CarProperties(speedKmh = 0.0)

    private fun ev(kwh: Double, odo: Double, gear: Gear = Gear.PARK, port: Boolean = false) = VehicleSnapshot(
        cumulativeDistanceKm = odo, gear = gear, chargePortOpen = port,
        batteryEnergyKwh = kwh, batteryCapacityKwh = 77.4, batteryPercent = kwh / 77.4 * 100,
    )

    private suspend fun signal(s: VehicleSnapshot, after: Duration = Duration.ofMinutes(1)) {
        clock.advance(after)
        monitor.onSignal(s, car, DataSourceMode.DEMO)
    }

    @Test
    fun `registers an EV on the first odometer reading`() = runTest {
        signal(ev(30.0, 12_345.0))

        val vehicle = vehicles.current()!!
        assertEquals(EnergyType.EV, vehicle.energyType)
        assertEquals(12_345.0, vehicle.baselineOdometerKm, 0.0)
        assertEquals(77.4, vehicle.batteryCapacityKwh!!, 0.0)
        assertNotNull(vehicle.vinHash)
    }

    @Test
    fun `drive then charge gives one measured session and one charge draft`() = runTest {
        signal(ev(30.0, 12_345.0))
        signal(ev(30.0, 12_345.0, Gear.DRIVE))
        signal(ev(26.0, 12_370.0, Gear.DRIVE), Duration.ofMinutes(30))
        signal(ev(25.5, 12_372.0, Gear.PARK))
        signal(ev(25.5, 12_372.0, Gear.PARK, port = true))
        signal(ev(45.5, 12_372.0, Gear.PARK, port = true), Duration.ofMinutes(40))
        signal(ev(45.5, 12_372.0, Gear.PARK, port = false))

        val session = sessions.all.value.values.single()
        assertEquals(SessionKind.MEASURED, session.kind)
        assertEquals(27.0, session.distanceKm, 1e-9)
        assertEquals(4.5, session.energyUsed!!, 1e-9)
        assertNotNull(session.end)

        val draft = records.all.value.values.single()
        assertEquals(RecordStatus.DRAFT, draft.status)
        assertEquals(StampSource.DEMO, draft.stamp.source)
        assertEquals(12_372.0, draft.stamp.odometerKm, 0.0)
        assertEquals(20.0, (draft.detail as RecordDetail.Energy).amount!!, 1e-9)
        assertEquals(6_000L, draft.amountWon)
    }

    @Test
    fun `a charge resumed within 30 minutes updates the same draft`() = runTest {
        signal(ev(20.0, 100.0))
        signal(ev(20.0, 100.0, port = true))
        signal(ev(30.0, 100.0, port = true), Duration.ofMinutes(20))
        signal(ev(30.0, 100.0, port = false))
        signal(ev(30.0, 100.0, port = true), Duration.ofMinutes(5))
        signal(ev(35.0, 100.0, port = true), Duration.ofMinutes(10))
        signal(ev(35.0, 100.0, port = false))

        val draft = records.all.value.values.single()
        assertEquals(15.0, (draft.detail as RecordDetail.Energy).amount!!, 1e-9)
    }

    @Test
    fun `distance driven while the app was off is stored as a gap before the new drive`() = runTest {
        signal(ev(30.0, 1_000.0))
        signal(ev(30.0, 1_000.0, Gear.DRIVE))
        signal(ev(29.0, 1_005.0, Gear.PARK))
        signal(ev(29.0, 1_005.0, Gear.PARK), Duration.ofMinutes(3))
        assertEquals(1, sessions.all.value.size)

        // The app process ended; the car went 40 km before it started again.
        monitor = newMonitor()
        signal(ev(22.0, 1_045.0, Gear.PARK), Duration.ofHours(5))
        signal(ev(22.0, 1_045.0, Gear.DRIVE))

        val gap = sessions.all.value.values.single { it.kind == SessionKind.GAP }
        assertEquals(40.0, gap.distanceKm, 1e-9)
        assertEquals(1_005.0, gap.start.odometerKm, 1e-9)
        val open = sessions.all.value.values.single { it.end == null }
        assertEquals(1_045.0, open.start.odometerKm, 1e-9)
    }

    @Test
    fun `switching to the real vehicle does not count the odometer jump as a drive`() = runTest {
        signal(ev(30.0, 23_410.0))
        clock.advance(Duration.ofMinutes(1))
        monitor.onSignal(ev(50.0, 12_345.0), car, DataSourceMode.REAL)
        clock.advance(Duration.ofMinutes(1))
        monitor.onSignal(ev(50.0, 12_346.0), car, DataSourceMode.REAL)

        val started = sessions.all.value.values.single()
        assertEquals(12_345.0, started.start.odometerKm, 0.0)
        assertEquals(StampSource.VEHICLE, started.start.source)
        assertNull(records.all.value.values.firstOrNull())
    }
}
