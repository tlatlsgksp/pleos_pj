package com.example.pleos.ui.feature.dashboard

import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.ui.feature.care.CareItemUi

/** A detected charge or refuel waiting for the user. */
data class PendingChargeUi(
    val recordId: String,
    /** kWh for EV, L for fuel. */
    val amount: Double?,
    val isEv: Boolean,
    val estimatedWon: Long?,
)

/** Immutable screen state. null = unknown or unsupported on this vehicle. */
data class DashboardState(
    val batteryPercent: Int? = null,
    val fuelPercent: Int? = null,
    val rangeKm: Int? = null,
    val odometerKm: Double? = null,
    val drivingMode: DrivingMode? = null,
    val warnings: Set<VehicleWarning> = emptySet(),
    val airbagDeployed: Boolean = false,
    val pendingCharge: PendingChargeUi? = null,
    /** Most urgent care items, at most [CARE_PREVIEW_COUNT]. */
    val careItems: List<CareItemUi> = emptyList(),
    val isDemo: Boolean = false,
    val isDemoPlaying: Boolean = false,
) {
    companion object {
        const val CARE_PREVIEW_COUNT = 3
    }
}

sealed interface DashboardEvent {
    // User input
    data object PlayDemoTapped : DashboardEvent
    data object StopDemoTapped : DashboardEvent
    data object OpenCareTapped : DashboardEvent
    data object OpenChargeTapped : DashboardEvent
    /** Switch between the real vehicle and the demo scenario. */
    data object SourceToggleTapped : DashboardEvent

    // System input, raised by the ViewModel from collected flows
    data class DashboardChanged(val data: DashboardData) : DashboardEvent
    data class DemoPlaybackChanged(val playing: Boolean) : DashboardEvent
    data class PendingChargeChanged(val charge: PendingChargeUi?) : DashboardEvent
    data class CareChanged(val items: List<CareItemUi>) : DashboardEvent
}

/** One-shot effects consumed exactly once by the route. */
sealed interface DashboardEffect {
    data class ShowMessage(val text: String) : DashboardEffect
    data object NavigateToCare : DashboardEffect
    data class NavigateToCharge(val recordId: String) : DashboardEffect
}
