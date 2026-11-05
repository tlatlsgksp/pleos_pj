package com.example.pleos.platform.demo

import com.example.pleos.domain.drive.VehicleMonitor
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.SessionKind
import com.example.pleos.domain.testing.InMemoryDriveSessionRepository
import com.example.pleos.domain.testing.InMemoryRecordRepository
import com.example.pleos.domain.testing.InMemorySettingsRepository
import com.example.pleos.domain.testing.InMemoryVehicleRepository
import com.example.pleos.domain.testing.MutableClock
import com.example.pleos.domain.testing.SequentialIds
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeVehicleGateway
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvDemoScenarioTest {

    @Test
    fun `the EV demo leaves one charge draft, a measured drive and a higher odometer`() = runTest(UnconfinedTestDispatcher()) {
        val scenario = DemoScenario.evCoreFlow()
        val vehicle = FakeVehicleGateway(scenario.initialVehicle)
        val car = FakeCarPropertySource(scenario.initialCar)
        val clock = MutableClock()
        val sessions = InMemoryDriveSessionRepository()
        val records = InMemoryRecordRepository()
        val monitor = VehicleMonitor(
            vehicle, car, InMemoryVehicleRepository(), sessions, records,
            InMemorySettingsRepository(), SequentialIds(), clock,
        )
        backgroundScope.launch { monitor.run() }

        scenario.steps.forEach { step ->
            clock.advance(Duration.ofMillis(step.delayMillis))
            step.apply(vehicle, car)
        }

        val draft = records.all.value.values.single()
        assertEquals(RecordStatus.DRAFT, draft.status)
        assertEquals(40.0, (draft.detail as RecordDetail.Energy).amount!!, 1e-9)
        assertEquals(12_370.0, draft.stamp.odometerKm, 1e-9)

        val drives = sessions.all.value.values.filter { it.kind == SessionKind.MEASURED }
        assertEquals(25.0, drives.single { it.end != null }.distanceKm, 1e-9)
        assertTrue(drives.any { it.end == null })
        assertEquals(24_370.0, vehicle.current.cumulativeDistanceKm!!, 1e-9)
    }
}
