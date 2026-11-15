package com.example.pleos.domain.eco

import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.testing.InMemorySettingsRepository
import com.example.pleos.domain.testing.MutableCarPropertySource
import com.example.pleos.domain.testing.MutableClock
import com.example.pleos.domain.testing.MutableVehicleGateway
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EcoDriveTest {

    @Test
    fun `hard starts and stops and high speed are judged by how much`() {
        assertEquals(EcoVerdict.GOOD, EcoDriveRules.judge(50.0, 54.0, 1.0))
        assertEquals(EcoVerdict(EcoLevel.CAUTION, EcoReason.HARD_ACCELERATION), EcoDriveRules.judge(50.0, 57.0, 1.0))
        assertEquals(EcoVerdict(EcoLevel.BAD, EcoReason.HARD_ACCELERATION), EcoDriveRules.judge(50.0, 70.0, 1.0))
        assertEquals(EcoVerdict(EcoLevel.CAUTION, EcoReason.HARD_BRAKING), EcoDriveRules.judge(60.0, 51.0, 1.0))
        assertEquals(EcoVerdict(EcoLevel.BAD, EcoReason.HARD_BRAKING), EcoDriveRules.judge(60.0, 40.0, 1.0))
        assertEquals(EcoVerdict(EcoLevel.CAUTION, EcoReason.HIGH_SPEED), EcoDriveRules.judge(104.0, 105.0, 5.0))
        assertEquals(EcoVerdict(EcoLevel.BAD, EcoReason.HIGH_SPEED), EcoDriveRules.judge(124.0, 125.0, 5.0))
    }

    @Test
    fun `the worse of the speed change and the speed wins, and no history means no change`() {
        // Gaining 9 km/h a second is CAUTION, but 125 km/h is BAD.
        assertEquals(EcoVerdict(EcoLevel.BAD, EcoReason.HIGH_SPEED), EcoDriveRules.judge(116.0, 125.0, 1.0))
        assertEquals(EcoVerdict.GOOD, EcoDriveRules.judge(null, 80.0, null))
        // Samples too close together are noise.
        assertEquals(EcoVerdict.GOOD, EcoDriveRules.judge(50.0, 52.0, 0.1))
    }

    private class Rig(private val scope: TestScope) {
        val clock = MutableClock()
        val car = MutableCarPropertySource(CarProperties(speedKmh = 50.0))
        val vehicle = RecordingAmbientGateway()
        val settings = InMemorySettingsRepository()
        val guide = EcoDriveGuide(car, vehicle, settings, clock)

        init {
            scope.backgroundScope.launch { guide.run() }
        }

        fun speed(kmh: Double, afterMillis: Long) {
            clock.advance(Duration.ofMillis(afterMillis))
            car.state.value = CarProperties(speedKmh = kmh)
            scope.runCurrent()
        }
    }

    @Test
    fun `with the guide on a hard start turns the lights red, then back to the driver's color`() = runTest(UnconfinedTestDispatcher()) {
        val rig = Rig(this)
        rig.settings.setAmbientBaseColor(0x2979FF)
        rig.guide.setEnabled(true)

        rig.speed(70.0, afterMillis = 1_000)
        assertEquals(EcoLevel.BAD, rig.guide.state.value.verdict.level)
        assertEquals(listOf(EcoDriveGuide.BAD_COLOR), rig.vehicle.colors)

        // Calm again, but the warning holds for 3 seconds before the lights go back.
        rig.speed(72.0, afterMillis = 2_000)
        assertEquals(listOf(EcoDriveGuide.BAD_COLOR), rig.vehicle.colors)
        advanceTimeBy(3_001)
        assertEquals(EcoLevel.GOOD, rig.guide.state.value.verdict.level)
        assertEquals(listOf(EcoDriveGuide.BAD_COLOR, 0x2979FF), rig.vehicle.colors)
    }

    @Test
    fun `with the guide off nothing is sent, and turning it off mid-warning restores the lights`() = runTest(UnconfinedTestDispatcher()) {
        val rig = Rig(this)
        rig.speed(70.0, afterMillis = 1_000)
        assertEquals(EcoLevel.BAD, rig.guide.state.value.verdict.level)
        assertTrue(rig.vehicle.colors.isEmpty())
        advanceTimeBy(3_001)

        // Turning it on while driving calmly leaves the driver's lights alone.
        rig.guide.setEnabled(true)
        assertTrue(rig.vehicle.colors.isEmpty())
        rig.speed(105.0, afterMillis = 10_000)
        assertEquals(listOf(EcoDriveGuide.CAUTION_COLOR), rig.vehicle.colors)

        rig.guide.setEnabled(false)
        assertEquals(listOf(EcoDriveGuide.CAUTION_COLOR, SettingsRepository.DEFAULT_AMBIENT_COLOR), rig.vehicle.colors)
    }
}

/** Records every mood light color sent. */
private class RecordingAmbientGateway : com.example.pleos.domain.gateway.VehicleGateway by MutableVehicleGateway(
    com.example.pleos.domain.model.VehicleSnapshot(),
) {
    val colors = mutableListOf<Int>()

    override suspend fun setAmbientColor(rgb: Int): Boolean {
        colors += rgb
        return true
    }
}
