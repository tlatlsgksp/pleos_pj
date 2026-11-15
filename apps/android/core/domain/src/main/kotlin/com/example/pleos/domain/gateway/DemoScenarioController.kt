package com.example.pleos.domain.gateway

import kotlinx.coroutines.flow.Flow

/** Plays the demo scenario against the Fake gateways. Only meaningful in [com.example.pleos.domain.model.DataSourceMode.DEMO]. */
interface DemoScenarioController {
    val isPlaying: Flow<Boolean>

    fun play()

    fun stop()
}
