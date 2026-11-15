package com.example.pleos.ui.feature.brand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.StationBrand
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BrandViewModel @Inject constructor(
    vehicles: VehicleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrandState())
    val state: StateFlow<BrandState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(vehicles.vehicle, settings.preferredBrands) { v, brands -> BrandEvent.Loaded(v?.energyType, brands) }
                .collect(::onEvent)
        }
    }

    fun onEvent(event: BrandEvent) {
        when (event) {
            is BrandEvent.Loaded -> _state.update {
                it.copy(groups = groupsFor(event.energyType), selected = event.selected, isLoading = false)
            }
            is BrandEvent.BrandTapped -> {
                val current = _state.value.selected
                val next = if (event.brand in current) current - event.brand else current + event.brand
                // Shown at once; the saved set comes back through Loaded.
                _state.update { it.copy(selected = next) }
                viewModelScope.launch { settings.setPreferredBrands(next) }
            }
        }
    }
}

internal fun groupsFor(energyType: EnergyType?): List<BrandGroupUi> =
    (energyType?.let(::listOf) ?: listOf(EnergyType.EV, EnergyType.FUEL))
        .map { BrandGroupUi(it, StationBrand.forEnergyType(it)) }
