package com.example.pleos.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.route.ChargeStopAdvisor
import com.example.pleos.domain.route.ChargeStopSuggestion
import com.example.pleos.domain.usecase.ObserveCareItemsUseCase
import com.example.pleos.domain.usecase.ObserveDashboardUseCase
import com.example.pleos.domain.usecase.ObservePendingDraftUseCase
import com.example.pleos.ui.feature.care.toUi
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
    observePendingDraft: ObservePendingDraftUseCase,
    observeCareItems: ObserveCareItemsUseCase,
    private val demo: DemoScenarioController,
    private val settings: SettingsRepository,
    private val chargeStops: ChargeStopAdvisor,
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
        viewModelScope.launch {
            observePendingDraft().collect { onEvent(DashboardEvent.PendingChargeChanged(it?.toPendingCharge())) }
        }
        viewModelScope.launch {
            chargeStops.suggestion.collect { onEvent(DashboardEvent.ChargeStopChanged(it?.toUi())) }
        }
        viewModelScope.launch {
            observeCareItems().collect { items ->
                onEvent(DashboardEvent.CareChanged(items.take(DashboardState.CARE_PREVIEW_COUNT).map { it.toUi() }))
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.DashboardChanged -> _state.update { reduce(it, event.data) }
            is DashboardEvent.DemoPlaybackChanged -> _state.update { it.copy(isDemoPlaying = event.playing) }
            is DashboardEvent.PendingChargeChanged -> {
                val previous = _state.value.pendingCharge
                _state.update { it.copy(pendingCharge = event.charge) }
                if (event.charge != null && event.charge.recordId != previous?.recordId) {
                    _effects.trySend(DashboardEffect.ShowMessage(if (event.charge.isEv) "충전을 감지했어요. 금액만 확인해 주세요" else "주유를 감지했어요. 금액만 확인해 주세요"))
                }
            }
            is DashboardEvent.CareChanged -> _state.update { it.copy(careItems = event.items) }
            is DashboardEvent.ChargeStopChanged -> _state.update { it.copy(chargeStop = event.stop) }
            DashboardEvent.AddChargeStopTapped -> {
                if (_state.value.isAddingStop || _state.value.chargeStop == null) return
                _state.update { it.copy(isAddingStop = true) }
                viewModelScope.launch { onEvent(DashboardEvent.ChargeStopAdded(chargeStops.accept())) }
            }
            is DashboardEvent.ChargeStopAdded -> {
                _state.update { it.copy(isAddingStop = false) }
                if (event.added) _effects.trySend(DashboardEffect.ShowMessage("충전소를 내비 경유지로 넣었어요"))
            }
            DashboardEvent.DismissChargeStopTapped -> chargeStops.dismiss()
            DashboardEvent.PlayDemoTapped -> {
                demo.play()
                _effects.trySend(DashboardEffect.ShowMessage("데모 시나리오를 시작했어요"))
            }
            DashboardEvent.StopDemoTapped -> demo.stop()
            DashboardEvent.SourceToggleTapped -> {
                val target = if (_state.value.isDemo) DataSourceMode.REAL else DataSourceMode.DEMO
                if (target == DataSourceMode.REAL) demo.stop()
                viewModelScope.launch { settings.setDataSourceMode(target) }
            }
            DashboardEvent.OpenCareTapped -> _effects.trySend(DashboardEffect.NavigateToCare)
            DashboardEvent.OpenChargeTapped -> _state.value.pendingCharge?.let {
                _effects.trySend(DashboardEffect.NavigateToCharge(it.recordId))
            }
        }
    }
}

/** Pure reducer: maps the latest dashboard data onto the screen state. */
internal fun reduce(state: DashboardState, data: DashboardData): DashboardState = state.copy(
    batteryPercent = data.vehicle.batteryPercent?.roundToInt(),
    fuelPercent = data.vehicle.fuelPercent,
    rangeKm = data.estimatedRangeKm?.roundToInt(),
    odometerKm = data.vehicle.cumulativeDistanceKm,
    drivingMode = data.vehicle.drivingMode,
    warnings = data.vehicle.warnings,
    airbagDeployed = data.vehicle.airbagDeployed == true,
    isDemo = data.source == DataSourceMode.DEMO,
)

internal fun Record.toPendingCharge(): PendingChargeUi? {
    val energy = detail as? RecordDetail.Energy ?: return null
    return PendingChargeUi(id, energy.amount, energy.kind == EnergyType.EV, amountWon)
}

internal fun ChargeStopSuggestion.toUi() = ChargeStopUi(
    name = candidate.place.name,
    distanceKm = candidate.distanceKm,
    rangeKm = rangeKm,
    remainingKm = remainingKm,
    destinationName = destinationName,
    isDemoData = candidate.isDemoData,
)
