package com.example.pleos.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.usecase.ObserveDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    private val demo: DemoScenarioController,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeDashboard().collect { onEvent(DashboardEvent.DashboardChanged(it)) }
        }
        viewModelScope.launch {
            demo.isPlaying.collect { onEvent(DashboardEvent.DemoPlaybackChanged(it)) }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.DashboardChanged -> _state.update { reduce(it, event.data) }
            is DashboardEvent.DemoPlaybackChanged -> _state.update { it.copy(isDemoPlaying = event.playing) }
            DashboardEvent.PlayDemoTapped -> {
                demo.play()
                _effects.trySend(DashboardEffect.ShowMessage("데모 시나리오를 시작했어요"))
            }
            DashboardEvent.StopDemoTapped -> demo.stop()
        }
    }
}

/** Pure reducer: maps the latest dashboard data onto the screen state. */
internal fun reduce(state: DashboardState, data: DashboardData): DashboardState = state.copy(
    fuelPercent = data.vehicle.fuelPercent,
    rangeKm = data.estimatedRangeKm?.roundToInt(),
    drivingMode = data.vehicle.drivingMode,
    tripSinceRefillKm = data.vehicle.tripSinceRefillKm,
    warnings = data.vehicle.warnings,
    airbagDeployed = data.vehicle.airbagDeployed == true,
    isDemo = data.source == DataSourceMode.DEMO,
)
