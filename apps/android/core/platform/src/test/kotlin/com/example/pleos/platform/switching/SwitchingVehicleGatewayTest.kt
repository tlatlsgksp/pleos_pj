package com.example.pleos.platform.switching

import app.cash.turbine.test
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.platform.fake.FakeVehicleGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SwitchingVehicleGatewayTest {

    @Test
    fun `falls back to the fake and reports DEMO when no real gateway is bound`() = runTest {
        val mode = MutableStateFlow(DataSourceMode.REAL)
        val fake = FakeVehicleGateway(VehicleSnapshot(fuelPercent = 33))
        val gateway = SwitchingVehicleGateway(mode, real = null, fake = fake)

        assertFalse(gateway.isRealAvailable)
        gateway.activeSource.test {
            assertEquals(DataSourceMode.DEMO, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        gateway.snapshot.test {
            assertEquals(33, awaitItem().fuelPercent)
            fake.setFuelPercent(34)
            assertEquals(34, awaitItem().fuelPercent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switches between real and fake when the mode changes`() = runTest {
        val mode = MutableStateFlow(DataSourceMode.DEMO)
        val real = FakeVehicleGateway(VehicleSnapshot(fuelPercent = 70))
        val fake = FakeVehicleGateway(VehicleSnapshot(fuelPercent = 20))
        val gateway = SwitchingVehicleGateway(mode, real = real, fake = fake)

        gateway.snapshot.test {
            assertEquals(20, awaitItem().fuelPercent)
            mode.value = DataSourceMode.REAL
            assertEquals(70, awaitItem().fuelPercent)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
