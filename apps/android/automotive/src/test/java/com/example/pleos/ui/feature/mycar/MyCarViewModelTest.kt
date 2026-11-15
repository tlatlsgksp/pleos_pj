package com.example.pleos.ui.feature.mycar

import app.cash.turbine.test
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.StationBrand
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.ui.testing.TestApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyCarViewModelTest {

    private val app = TestApp()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MyCarViewModel(app.vehicles, app.gateway, app.settings, app.demo, com.example.pleos.platform.demo.DemoCommunityRepository(app.clock))

    @Test
    fun `shows the car and the saved settings`() = runTest {
        app.settings.setPreferredBrands(setOf(StationBrand.KEPCO, StationBrand.EPIT))
        app.settings.setEcoGuideEnabled(true)
        val vm = viewModel()

        val s = vm.state.value
        assertEquals(EnergyType.EV, s.energyType)
        assertEquals(77.4, s.capacity!!, 0.0)
        assertEquals(listOf("E-pit", "한국전력"), s.preferredBrands)
        assertTrue(s.ecoGuideEnabled)
    }

    @Test
    fun `switches and the price stepper save at once`() = runTest {
        val vm = viewModel()

        vm.onEvent(MyCarEvent.EcoGuideSwitched(true))
        vm.onEvent(MyCarEvent.ConsentSwitched(true))
        vm.onEvent(MyCarEvent.PriceStepped(up = true))

        assertTrue(app.settings.ecoGuideEnabled.first())
        assertTrue(app.settings.consentGiven.first())
        assertEquals(SettingsRepository.DEFAULT_CHARGE_UNIT_PRICE_WON + MyCarViewModel.PRICE_STEP, app.settings.chargeUnitPriceWon.first(), 1e-9)
    }

    @Test
    fun `the demo switch asks for the real vehicle and back, and plays the scenario`() = runTest {
        val vm = viewModel()
        app.settings.dataSourceMode.test {
            assertEquals(DataSourceMode.DEMO, awaitItem())
            vm.onEvent(MyCarEvent.SourceToggleTapped)
            assertEquals(DataSourceMode.REAL, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        vm.effects.test {
            vm.onEvent(MyCarEvent.PlayDemoTapped)
            assertEquals(1, app.demo.playCalls)
            assertTrue(vm.state.value.isDemoPlaying)
            assertTrue(awaitItem() is MyCarEffect.ShowMessage)
        }
    }

    @Test
    fun `a row opens its detail, back closes it, and the skin opens its own screen`() = runTest {
        val vm = viewModel()
        vm.onEvent(MyCarEvent.SectionTapped(MyCarSection.BRANDS))
        assertEquals(MyCarSection.BRANDS, vm.state.value.selected)
        vm.onEvent(MyCarEvent.BackTapped)
        assertEquals(null, vm.state.value.selected)

        vm.effects.test {
            vm.onEvent(MyCarEvent.OpenSkinTapped)
            assertEquals(MyCarEffect.NavigateToSkin, awaitItem())
        }
    }
}
