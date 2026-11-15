package com.example.pleos.ui.feature.skin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SkinEffect {
    data class ShowMessage(val text: String) : SkinEffect
}

/**
 * The car skin studio is a bundled web page (assets/skin); this ViewModel only carries its mood
 * light choice to the car. The page keeps the skin itself.
 */
@HiltViewModel
class SkinViewModel @Inject constructor(
    private val vehicle: VehicleGateway,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _effects = Channel<SkinEffect>(Channel.BUFFERED)
    val effects: Flow<SkinEffect> = _effects.receiveAsFlow()

    /** [hex] is RRGGBB from the page. Called from the WebView's bridge thread. */
    fun onAmbientColor(hex: String) {
        val rgb = hex.toIntOrNull(16)?.takeIf { hex.length == 6 } ?: return
        viewModelScope.launch {
            // The eco guide returns to this color after a warning.
            settings.setAmbientBaseColor(rgb)
            val applied = vehicle.setAmbientColor(rgb)
            _effects.trySend(
                SkinEffect.ShowMessage(if (applied) "실내 무드등을 #$hex 색으로 바꿨어요" else "이 차에서는 무드등을 바꾸지 못했어요"),
            )
        }
    }
}
