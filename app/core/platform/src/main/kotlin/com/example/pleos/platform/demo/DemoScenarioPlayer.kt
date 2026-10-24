package com.example.pleos.platform.demo

import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.platform.di.DemoScope
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeVehicleGateway
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Runs a [DemoScenario] step by step against the Fake gateways. */
@Singleton
internal class DemoScenarioPlayer @Inject constructor(
    private val vehicle: FakeVehicleGateway,
    private val car: FakeCarPropertySource,
    @DemoScope private val scope: CoroutineScope,
) : DemoScenarioController {

    var scenario: DemoScenario = DemoScenario.default()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: Flow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow<DemoStep?>(null)
    val currentStep: Flow<DemoStep?> = _currentStep.asStateFlow()

    private var job: Job? = null

    override fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        vehicle.reset(scenario.initialVehicle)
        car.reset(scenario.initialCar)
        job = scope.launch {
            try {
                scenario.steps.forEach { step ->
                    delay(step.delayMillis)
                    step.apply(vehicle, car)
                    _currentStep.value = step
                }
            } finally {
                _isPlaying.value = false
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _currentStep.value = null
        _isPlaying.value = false
    }
}
