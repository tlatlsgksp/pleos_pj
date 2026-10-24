package com.example.pleos.ui.feature.dashboard

import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.VehicleWarning

/** Immutable screen state. null = unknown or unsupported on this vehicle. */
data class DashboardState(
    val fuelPercent: Int? = null,
    val rangeKm: Int? = null,
    val drivingMode: DrivingMode? = null,
    val tripSinceRefillKm: Double? = null,
    val warnings: Set<VehicleWarning> = emptySet(),
    val airbagDeployed: Boolean = false,
    val isDemo: Boolean = false,
    val isDemoPlaying: Boolean = false,
)

sealed interface DashboardEvent {
    // User input
    data object PlayDemoTapped : DashboardEvent
    data object StopDemoTapped : DashboardEvent

    // System input, raised by the ViewModel from collected flows
    data class DashboardChanged(val data: DashboardData) : DashboardEvent
    data class DemoPlaybackChanged(val playing: Boolean) : DashboardEvent
}

/** One-shot effects consumed exactly once by the route. */
sealed interface DashboardEffect {
    data class ShowMessage(val text: String) : DashboardEffect
}
