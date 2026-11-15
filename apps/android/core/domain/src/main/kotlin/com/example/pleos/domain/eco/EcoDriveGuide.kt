package com.example.pleos.domain.eco

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.repository.SettingsRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the home card shows. */
data class EcoGuideState(val enabled: Boolean = false, val verdict: EcoVerdict = EcoVerdict.GOOD)

/**
 * Eco driving guide: judges every speed change with [EcoDriveRules] and, when the driver turned the
 * guide on, tints the cabin mood lights orange or red while the driving costs energy. A light cue
 * needs no glance at the screen. Back to the driver's own mood light color once driving is calm.
 * Runs for the life of the app process, like [com.example.pleos.domain.route.ChargeStopAdvisor].
 */
@Singleton
class EcoDriveGuide @Inject constructor(
    private val car: CarPropertySource,
    private val vehicle: VehicleGateway,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val _state = MutableStateFlow(EcoGuideState())
    val state: StateFlow<EcoGuideState> = _state.asStateFlow()

    /** The color last sent, so the same color is not sent twice. null = untouched. */
    @Volatile private var shownColor: Int? = null

    suspend fun run() = coroutineScope {
        launch {
            settings.ecoGuideEnabled.distinctUntilChanged().collect { enabled ->
                _state.update { it.copy(enabled = enabled) }
                // Turning the guide off hands the lights back at once.
                if (!enabled && shownColor != null && shownColor != settings.ambientBaseColor.first()) {
                    show(settings.ambientBaseColor.first())
                }
                if (enabled) applyLights()
            }
        }
        var previous: Pair<Double, Instant>? = null
        var warnUntil: Instant? = null
        var calmDown: Job? = null
        car.properties.mapNotNull { it.speedKmh }.distinctUntilChanged().collect { speed ->
            val now = clock.instant()
            val interval = previous?.let { Duration.between(it.second, now).toMillis() / 1_000.0 }
            val verdict = EcoDriveRules.judge(previous?.first, speed, interval)
            previous = speed to now
            calmDown?.cancel()
            if (verdict.level != EcoLevel.GOOD) {
                warnUntil = now.plus(HOLD)
                setVerdict(verdict)
            }
            // Hold a warning long enough to be noticed, and so the lights do not flicker. A steady
            // speed sends no new sample, so after the hold judge the speed alone (a hard start
            // is over by then; 125 km/h is not).
            val wait = warnUntil?.let { Duration.between(now, it).toMillis() } ?: 0
            if (wait > 0) {
                calmDown = launch {
                    delay(wait)
                    setVerdict(EcoDriveRules.judge(null, speed, null))
                }
            } else {
                setVerdict(verdict)
            }
        }
    }

    suspend fun setEnabled(enabled: Boolean) = settings.setEcoGuideEnabled(enabled)

    private suspend fun setVerdict(verdict: EcoVerdict) {
        if (_state.value.verdict == verdict) return
        _state.update { it.copy(verdict = verdict) }
        if (_state.value.enabled) applyLights()
    }

    private suspend fun applyLights() {
        val color = when (_state.value.verdict.level) {
            EcoLevel.GOOD -> settings.ambientBaseColor.first()
            EcoLevel.CAUTION -> CAUTION_COLOR
            EcoLevel.BAD -> BAD_COLOR
        }
        // Nothing to undo before the first warning; leave the driver's lights alone until then.
        if (shownColor == null && _state.value.verdict.level == EcoLevel.GOOD) return
        if (color != shownColor) show(color)
    }

    private suspend fun show(color: Int) {
        shownColor = color
        vehicle.setAmbientColor(color)
    }

    companion object {
        const val CAUTION_COLOR = 0xFF8F00
        const val BAD_COLOR = 0xFF1744
        private val HOLD: Duration = Duration.ofSeconds(3)
    }
}
