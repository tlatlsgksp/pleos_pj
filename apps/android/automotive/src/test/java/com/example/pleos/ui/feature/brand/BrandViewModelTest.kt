package com.example.pleos.ui.feature.brand

import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.StationBrand
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrandViewModelTest {

    private val app = TestApp()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an EV shows only charging brands`() = runTest {
        val vm = BrandViewModel(app.vehicles, app.settings)

        assertEquals(listOf(EnergyType.EV), vm.state.value.groups.map { it.energyType })
        assertEquals(StationBrand.forEnergyType(EnergyType.EV), vm.state.value.groups.single().brands)
    }

    @Test
    fun `tapping toggles a brand and saves the set`() = runTest {
        val vm = BrandViewModel(app.vehicles, app.settings)

        vm.onEvent(BrandEvent.BrandTapped(StationBrand.EPIT))
        vm.onEvent(BrandEvent.BrandTapped(StationBrand.KEPCO))
        vm.onEvent(BrandEvent.BrandTapped(StationBrand.EPIT))

        assertEquals(setOf(StationBrand.KEPCO), vm.state.value.selected)
        assertEquals(setOf(StationBrand.KEPCO), app.settings.preferredBrands.first())
    }

    @Test
    fun `an unknown vehicle shows both groups`() {
        assertEquals(listOf(EnergyType.EV, EnergyType.FUEL), groupsFor(null).map { it.energyType })
    }
}
