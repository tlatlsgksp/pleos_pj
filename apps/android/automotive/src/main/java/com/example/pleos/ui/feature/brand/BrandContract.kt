package com.example.pleos.ui.feature.brand

import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.StationBrand

/** Brands of one energy type, shown as one section. */
data class BrandGroupUi(val energyType: EnergyType, val brands: List<StationBrand>)

data class BrandState(
    /** The vehicle's type only; both while the vehicle is not known yet. */
    val groups: List<BrandGroupUi> = emptyList(),
    val selected: Set<StationBrand> = emptySet(),
    val isLoading: Boolean = true,
)

sealed interface BrandEvent {
    data class BrandTapped(val brand: StationBrand) : BrandEvent

    data class Loaded(val energyType: EnergyType?, val selected: Set<StationBrand>) : BrandEvent
}
