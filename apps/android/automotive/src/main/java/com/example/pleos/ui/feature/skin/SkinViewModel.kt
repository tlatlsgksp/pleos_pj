package com.example.pleos.ui.feature.skin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A paint the studio offers: Ioniq 5 factory colors, named as the brochure names them. */
data class PaintOption(val name: String, val rgb: Int)

/** Studio state: what the viewer shows now, and what is saved, so the save button knows when to light. */
data class SkinState(
    val paint: Int = SettingsRepository.DEFAULT_BODY_PAINT,
    val ambient: Int = SettingsRepository.DEFAULT_AMBIENT_COLOR,
    val savedPaint: Int = SettingsRepository.DEFAULT_BODY_PAINT,
    val savedAmbient: Int = SettingsRepository.DEFAULT_AMBIENT_COLOR,
    val isSaving: Boolean = false,
) {
    val dirty: Boolean get() = paint != savedPaint || ambient != savedAmbient
}

sealed interface SkinEvent {
    data class PaintPicked(val rgb: Int) : SkinEvent
    data class AmbientPicked(val rgb: Int) : SkinEvent
    data object SaveTapped : SkinEvent
}

sealed interface SkinEffect {
    data class ShowMessage(val text: String) : SkinEffect
}

/**
 * The car skin studio: body paint and mood light. A pick shows at once in the viewer and on the
 * car's own lights; "저장" keeps it for home and the eco guide.
 */
@HiltViewModel
class SkinViewModel @Inject constructor(
    private val vehicle: VehicleGateway,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SkinState())
    val state: StateFlow<SkinState> = _state.asStateFlow()

    private val _effects = Channel<SkinEffect>(Channel.BUFFERED)
    val effects: Flow<SkinEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val (paint, ambient) = combine(settings.bodyPaint, settings.ambientBaseColor) { p, a -> p to a }.first()
            _state.update { it.copy(paint = paint, ambient = ambient, savedPaint = paint, savedAmbient = ambient) }
        }
    }

    fun onEvent(event: SkinEvent) {
        when (event) {
            is SkinEvent.PaintPicked -> _state.update { it.copy(paint = event.rgb) }
            is SkinEvent.AmbientPicked -> {
                _state.update { it.copy(ambient = event.rgb) }
                // Preview on the real cabin lights; the saved base color is what the eco guide returns to.
                viewModelScope.launch { vehicle.setAmbientColor(event.rgb) }
            }
            SkinEvent.SaveTapped -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.dirty || s.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            settings.setBodyPaint(s.paint)
            settings.setAmbientBaseColor(s.ambient)
            val lit = vehicle.setAmbientColor(s.ambient)
            _state.update { it.copy(savedPaint = s.paint, savedAmbient = s.ambient, isSaving = false) }
            _effects.trySend(SkinEffect.ShowMessage(if (lit) "내 차를 저장했어요." else "내 차를 저장했어요. 이 차에서는 무드등은 바뀌지 않아요."))
        }
    }

    companion object {
        /** Ioniq 5 factory paints, sRGB approximations. */
        val PAINTS = listOf(
            PaintOption("루시드 블루", 0x3D6FA8),
            PaintOption("아틀라스 화이트", 0xE9EAEC),
            PaintOption("사이버 그레이", 0x8A8F97),
            PaintOption("팬텀 블랙", 0x1A1B1E),
            PaintOption("그래비티 골드", 0x7A6F5C),
            PaintOption("디지털 틸", 0x2E6E6A),
            PaintOption("슈팅스타 그레이", 0x5C6066),
            PaintOption("미스틱 올리브", 0x6B7A56),
        )

        /** Mood light colors; the same set home offers. */
        val AMBIENTS = listOf(0xFFFFFF, 0x9FD3F5, 0x5FA8E3, 0x7CE0B4, 0xFFD27A, 0xFF9E7A, 0xD7A6F5, 0xF5A6C8)
    }
}
