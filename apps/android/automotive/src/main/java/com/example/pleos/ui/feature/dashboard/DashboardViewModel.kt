package com.example.pleos.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.eco.EcoDriveGuide
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.route.ChargeStopAdvisor
import com.example.pleos.domain.route.ChargeStopSuggestion
import com.example.pleos.domain.usecase.ObserveDashboardUseCase
import com.example.pleos.domain.usecase.ObserveLedgerUseCase
import com.example.pleos.domain.usecase.ObservePendingDraftUseCase
import com.example.pleos.domain.usecase.monthSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
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
    observeLedger: ObserveLedgerUseCase,
    private val clock: Clock,
    private val chargeStops: ChargeStopAdvisor,
    private val ecoGuide: EcoDriveGuide,
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
            observePendingDraft().collect { onEvent(DashboardEvent.PendingChargeChanged(it?.toPendingCharge())) }
        }
        viewModelScope.launch {
            chargeStops.suggestion.collect { onEvent(DashboardEvent.ChargeStopChanged(it?.toUi())) }
        }
        viewModelScope.launch {
            ecoGuide.state.collect { onEvent(DashboardEvent.EcoGuideChanged(it)) }
        }
        viewModelScope.launch {
            observeLedger().collect { onEvent(DashboardEvent.MonthChanged(monthSummary(it.confirmed, clock.instant(), clock.zone))) }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.DashboardChanged -> _state.update { reduce(it, event.data) }
            is DashboardEvent.PendingChargeChanged -> {
                val previous = _state.value.pendingCharge
                _state.update { it.copy(pendingCharge = event.charge) }
                if (event.charge != null && event.charge.recordId != previous?.recordId) {
                    _effects.trySend(DashboardEffect.ShowMessage(if (event.charge.isEv) "충전을 감지했어요. 금액만 확인해 주세요" else "주유를 감지했어요. 금액만 확인해 주세요"))
                }
            }
            is DashboardEvent.ChargeStopChanged -> _state.update { it.copy(chargeStop = event.stop) }
            is DashboardEvent.MonthChanged -> _state.update { it.copy(month = event.month) }
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
            is DashboardEvent.EcoGuideChanged -> _state.update { it.copy(ecoGuide = event.state) }
            DashboardEvent.EcoGuideToggleTapped -> {
                val enable = !_state.value.ecoGuide.enabled
                viewModelScope.launch { ecoGuide.setEnabled(enable) }
                _effects.trySend(DashboardEffect.ShowMessage(if (enable) "연비 주행 가이드를 켰어요. 연비에 나쁜 주행이면 무드등이 주황·빨강으로 바뀌어요" else "연비 주행 가이드를 껐어요"))
            }
            DashboardEvent.OpenLedgerTapped -> _effects.trySend(DashboardEffect.NavigateToLedger)
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
    brand = candidate.brand?.label,
    isPreferredBrand = isPreferredBrand,
)
