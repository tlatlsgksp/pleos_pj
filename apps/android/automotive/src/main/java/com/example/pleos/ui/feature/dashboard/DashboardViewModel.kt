package com.example.pleos.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.eco.EcoDriveGuide
import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.ReportType
import com.example.pleos.domain.repository.CommunityRepository
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.route.ChargeStopAdvisor
import com.example.pleos.domain.route.ChargeStopSuggestion
import com.example.pleos.domain.usecase.ObserveDashboardUseCase
import com.example.pleos.domain.usecase.ObservePendingDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    observePendingDraft: ObservePendingDraftUseCase,
    private val chargeStops: ChargeStopAdvisor,
    private val ecoGuide: EcoDriveGuide,
    private val community: CommunityRepository,
    private val navigation: NavigationGateway,
    private val settings: SettingsRepository,
    private val vehicle: VehicleGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    private var bannerTimer: Job? = null

    init {
        viewModelScope.launch { observeDashboard().collect { onEvent(DashboardEvent.DashboardChanged(it)) } }
        viewModelScope.launch { observePendingDraft().collect { onEvent(DashboardEvent.PendingChargeChanged(it?.toPendingCharge())) } }
        viewModelScope.launch { chargeStops.suggestion.collect { onEvent(DashboardEvent.ChargeStopChanged(it?.toUi())) } }
        viewModelScope.launch { ecoGuide.state.collect { onEvent(DashboardEvent.EcoGuideChanged(it)) } }
        viewModelScope.launch { settings.ambientBaseColor.collect { onEvent(DashboardEvent.AmbientChanged(it)) } }
        viewModelScope.launch { settings.bodyPaint.collect { onEvent(DashboardEvent.BodyPaintChanged(it)) } }
        viewModelScope.launch {
            combine(community.me, community.ridings, community.ranking) { me, ridings, ranking ->
                DashboardEvent.CommunityChanged(me, ridings, ranking.myKmPerUnit, ranking.myRank)
            }.collect { onEvent(it) }
        }
        viewModelScope.launch { community.reportsOnRoute.collect { onEvent(DashboardEvent.ReportsChanged(it)) } }
        viewModelScope.launch {
            navigation.events.collect { event ->
                when (event) {
                    is NavEvent.RouteStarted -> community.setRouteActive(true)
                    NavEvent.RouteCancelled, NavEvent.DestinationArrived -> community.setRouteActive(false)
                    else -> Unit
                }
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.DashboardChanged -> _state.update { reduce(it, event.data) }
            is DashboardEvent.PendingChargeChanged -> {
                val isNew = event.charge != null && event.charge.recordId != _state.value.pendingCharge?.recordId
                _state.update { it.copy(pendingCharge = event.charge, pendingFresh = isNew || (it.pendingFresh && event.charge != null)) }
                if (isNew) viewModelScope.launch {
                    delay(BANNER_MS)
                    onEvent(DashboardEvent.PendingBannerExpired)
                }
            }
            DashboardEvent.PendingBannerExpired -> _state.update { it.copy(pendingFresh = false) }
            is DashboardEvent.ChargeStopChanged -> _state.update { it.copy(chargeStop = event.stop) }
            is DashboardEvent.EcoGuideChanged -> _state.update { it.copy(ecoGuide = event.state) }
            is DashboardEvent.AmbientChanged -> _state.update { it.copy(ambientColor = event.rgb) }
            is DashboardEvent.BodyPaintChanged -> _state.update { it.copy(bodyPaint = event.rgb) }
            is DashboardEvent.CommunityChanged -> _state.update {
                it.copy(me = event.me, rideCard = rideCard(event.ridings), myKmPerUnit = event.myKmPerUnit, myRank = event.myRank)
            }
            is DashboardEvent.ReportsChanged -> {
                val before = _state.value.reportsOnRoute.map { it.id }.toSet()
                val fresh = event.reports.firstOrNull { it.id !in before && it.aheadKm != null }
                _state.update { it.copy(reportsOnRoute = event.reports, freshReport = fresh ?: it.freshReport?.takeIf { f -> event.reports.any { r -> r.id == f.id } }) }
                if (fresh != null) {
                    bannerTimer?.cancel()
                    bannerTimer = viewModelScope.launch {
                        delay(BANNER_MS)
                        onEvent(DashboardEvent.FreshReportExpired)
                    }
                }
            }
            DashboardEvent.FreshReportExpired -> _state.update { it.copy(freshReport = null) }
            DashboardEvent.BannerTapped -> when (_state.value.banner) {
                is HomeBanner.RouteReport -> {
                    _state.update { it.copy(freshReport = null) }
                    _effects.trySend(DashboardEffect.OpenMap)
                }
                else -> Unit
            }

            DashboardEvent.ReportTapped -> _state.update { it.copy(reportSheetOpen = true) }
            DashboardEvent.ReportSheetDismissed -> _state.update { it.copy(reportSheetOpen = false) }
            is DashboardEvent.ReportPicked -> {
                _state.update { it.copy(reportSheetOpen = false) }
                viewModelScope.launch { community.report(event.type) }
                _effects.trySend(DashboardEffect.ShowMessage("${withObjectParticle(event.type.label())} 제보했어요."))
            }

            DashboardEvent.ChargerTapped -> {
                if (_state.value.isAddingStop) return
                if (_state.value.chargeStop == null) {
                    _effects.trySend(DashboardEffect.ShowMessage("지금은 가는 길에 들를 충전소가 없어요."))
                    return
                }
                _state.update { it.copy(isAddingStop = true) }
                viewModelScope.launch { onEvent(DashboardEvent.ChargeStopAdded(chargeStops.accept())) }
            }
            is DashboardEvent.ChargeStopAdded -> {
                _state.update { it.copy(isAddingStop = false) }
                if (event.added) _effects.trySend(DashboardEffect.ShowMessage("충전소를 경유지로 넣었어요."))
            }

            DashboardEvent.EcoGuideToggleTapped -> {
                val enable = !_state.value.ecoGuide.enabled
                viewModelScope.launch { ecoGuide.setEnabled(enable) }
                _effects.trySend(DashboardEffect.ShowMessage(if (enable) "연비 코치를 켰어요. 급가속하면 무드등이 주황색으로 바뀌어요." else "연비 코치를 껐어요."))
            }

            DashboardEvent.AmbientTapped -> _state.update { it.copy(ambientPickerOpen = true) }
            DashboardEvent.AmbientPickerDismissed -> _state.update { it.copy(ambientPickerOpen = false) }
            is DashboardEvent.AmbientPicked -> {
                _state.update { it.copy(ambientColor = event.rgb) }
                viewModelScope.launch {
                    settings.setAmbientBaseColor(event.rgb)
                    vehicle.setAmbientColor(event.rgb)
                }
            }

            DashboardEvent.RideCardTapped -> {
                val id = (_state.value.rideCard as? RideCardUi.In)?.riding?.id
                _effects.trySend(DashboardEffect.OpenRiding(id))
            }
            is DashboardEvent.JoinRideTapped -> viewModelScope.launch {
                community.join(event.ridingId)
                _effects.trySend(DashboardEffect.OpenRiding(event.ridingId))
            }
            is DashboardEvent.DeclineRideTapped -> viewModelScope.launch { community.decline(event.ridingId) }
            is DashboardEvent.LeaveRideTapped -> viewModelScope.launch {
                community.leave(event.ridingId)
                _effects.trySend(DashboardEffect.ShowMessage("그룹 라이딩에서 나왔어요. 언제든 다시 참여할 수 있어요."))
            }
            DashboardEvent.OpenChargeTapped -> _state.value.pendingCharge?.let {
                _effects.trySend(DashboardEffect.NavigateToCharge(it.recordId))
            }
        }
    }

    private companion object {
        const val BANNER_MS = 6_000L
    }
}

internal fun ReportType.label(): String = when (this) {
    ReportType.ACCIDENT -> "사고"
    ReportType.CONSTRUCTION -> "공사"
    ReportType.CONGESTION -> "정체"
    ReportType.SPEED_CAMERA -> "단속카메라"
    ReportType.DISASTER -> "재난"
}

/** "사고를", "재난을": the object particle that fits the last syllable. */
internal fun withObjectParticle(word: String): String {
    val last = word.lastOrNull() ?: return word
    val hasFinal = last in '가'..'힣' && (last - '가') % 28 != 0
    return word + if (hasFinal) "을" else "를"
}

/** Pure reducer: maps the latest dashboard data onto the screen state. */
internal fun reduce(state: DashboardState, data: DashboardData): DashboardState = state.copy(
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
