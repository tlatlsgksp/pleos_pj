package com.example.pleos.ui.feature.mycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.pleos.domain.repository.CommunityRepository

/** Personal settings: everything that fits the app to this car, these cards and this driver. */
@HiltViewModel
class MyCarViewModel @Inject constructor(
    vehicles: VehicleRepository,
    gateway: VehicleGateway,
    private val settings: SettingsRepository,
    private val demo: DemoScenarioController,
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyCarState())
    val state: StateFlow<MyCarState> = _state.asStateFlow()

    private val _effects = Channel<MyCarEffect>(Channel.BUFFERED)
    val effects: Flow<MyCarEffect> = _effects.receiveAsFlow()

    init {
        fun <T> follow(flow: Flow<T>, apply: MyCarState.(T) -> MyCarState) {
            viewModelScope.launch { flow.collect { value -> onEvent(MyCarEvent.Changed { it.apply(value) }) } }
        }
        follow(vehicles.vehicle) { v ->
            copy(
                energyType = v?.energyType,
                capacity = if (v?.energyType == EnergyType.EV) v.batteryCapacityKwh else v?.tankCapacityL,
                hasVin = v?.vinHash != null,
            )
        }
        follow(settings.ambientBaseColor) { copy(ambientColor = it) }
        follow(settings.ecoGuideEnabled) { copy(ecoGuideEnabled = it) }
        follow(settings.preferredBrands) { brands -> copy(preferredBrands = brands.sortedBy { it.ordinal }.map { it.label }) }
        follow(settings.chargeUnitPriceWon) { copy(chargeUnitPriceWon = it) }
        follow(settings.consentGiven) { copy(consentGiven = it) }
        follow(gateway.activeSource) { copy(isDemo = it == DataSourceMode.DEMO) }
        follow(demo.isPlaying) { copy(isDemoPlaying = it) }
        follow(community.me) { me -> copy(nickname = me.nickname, handle = me.handle, carModel = me.carModel, nicknameDraft = if (nicknameDraft.isEmpty()) me.nickname else nicknameDraft) }
    }

    fun onEvent(event: MyCarEvent) {
        when (event) {
            is MyCarEvent.Changed -> _state.update(event.transform)
            is MyCarEvent.SectionTapped -> _state.update { it.copy(selected = event.section) }
            MyCarEvent.BackTapped -> _state.update { it.copy(selected = null) }
            MyCarEvent.OpenSkinTapped -> _effects.trySend(MyCarEffect.NavigateToSkin)
            is MyCarEvent.EcoGuideSwitched -> viewModelScope.launch { settings.setEcoGuideEnabled(event.enabled) }
            is MyCarEvent.PriceStepped -> {
                val next = (_state.value.chargeUnitPriceWon + if (event.up) PRICE_STEP else -PRICE_STEP).coerceIn(MIN_PRICE, MAX_PRICE)
                _state.update { it.copy(chargeUnitPriceWon = next) }
                viewModelScope.launch { settings.setChargeUnitPriceWon(next) }
            }
            is MyCarEvent.ConsentSwitched -> viewModelScope.launch { settings.setConsentGiven(event.given) }
            MyCarEvent.SourceToggleTapped -> {
                val target = if (_state.value.isDemo) DataSourceMode.REAL else DataSourceMode.DEMO
                if (target == DataSourceMode.REAL) demo.stop()
                viewModelScope.launch { settings.setDataSourceMode(target) }
            }
            MyCarEvent.PlayDemoTapped -> {
                demo.play()
                _effects.trySend(MyCarEffect.ShowMessage("데모 시나리오를 시작했어요. 홈에서 보세요"))
            }
            MyCarEvent.StopDemoTapped -> demo.stop()
            is MyCarEvent.NicknameEdited -> _state.update { it.copy(nicknameDraft = event.text.take(NICKNAME_MAX)) }
            MyCarEvent.NicknameSaveTapped -> {
                val name = _state.value.nicknameDraft.trim()
                if (name.length < NICKNAME_MIN) {
                    _effects.trySend(MyCarEffect.ShowMessage("닉네임은 2자 이상이어야 해요."))
                    return
                }
                viewModelScope.launch { community.setNickname(name) }
                _effects.trySend(MyCarEffect.ShowMessage("닉네임을 바꿨어요."))
            }
        }
    }

    companion object {
        /** [기본값] Charger tariffs move in tens of won. */
        const val PRICE_STEP = 10.0
        const val MIN_PRICE = 100.0
        const val MAX_PRICE = 1_000.0
        const val NICKNAME_MIN = 2
        const val NICKNAME_MAX = 12
    }
}
