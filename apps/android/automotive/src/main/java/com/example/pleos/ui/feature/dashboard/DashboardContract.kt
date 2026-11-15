package com.example.pleos.ui.feature.dashboard

import com.example.pleos.domain.eco.EcoGuideState
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.domain.usecase.MonthSummary

/** A detected charge or refuel waiting for the user. */
data class PendingChargeUi(
    val recordId: String,
    /** kWh for EV, L for fuel. */
    val amount: Double?,
    val isEv: Boolean,
    val estimatedWon: Long?,
)

/** A charger offered as a waypoint because the range will not last the route. */
data class ChargeStopUi(
    val name: String,
    val distanceKm: Double,
    val rangeKm: Double,
    val remainingKm: Double?,
    val destinationName: String?,
    /** The bundled list is not real stations; the card says so. */
    val isDemoData: Boolean,
    val brand: String? = null,
    /** One of the driver's preferred brands. */
    val isPreferredBrand: Boolean = false,
)

/** The one card on the home screen that asks the driver for something, most urgent first. */
sealed interface HomeAction {
    data object Airbag : HomeAction
    data class ChargeStop(val stop: ChargeStopUi) : HomeAction
    data class PendingCharge(val charge: PendingChargeUi) : HomeAction
    data class Warnings(val warnings: Set<VehicleWarning>) : HomeAction
}

/** Immutable home state. null = unknown or unsupported on this vehicle. */
data class DashboardState(
    val batteryPercent: Int? = null,
    val fuelPercent: Int? = null,
    val rangeKm: Int? = null,
    val odometerKm: Double? = null,
    val drivingMode: DrivingMode? = null,
    val warnings: Set<VehicleWarning> = emptySet(),
    val airbagDeployed: Boolean = false,
    val pendingCharge: PendingChargeUi? = null,
    val chargeStop: ChargeStopUi? = null,
    val isAddingStop: Boolean = false,
    val ecoGuide: EcoGuideState = EcoGuideState(),
    val month: MonthSummary? = null,
) {
    /** Accident first, then the charger offer (time-bound), the draft to confirm, warnings. One at a time. */
    val action: HomeAction?
        get() = when {
            airbagDeployed -> HomeAction.Airbag
            chargeStop != null -> HomeAction.ChargeStop(chargeStop)
            pendingCharge != null -> HomeAction.PendingCharge(pendingCharge)
            warnings.isNotEmpty() -> HomeAction.Warnings(warnings)
            else -> null
        }

}

sealed interface DashboardEvent {
    // User input
    data object OpenLedgerTapped : DashboardEvent
    data object OpenChargeTapped : DashboardEvent
    data object AddChargeStopTapped : DashboardEvent
    data object DismissChargeStopTapped : DashboardEvent
    data object EcoGuideToggleTapped : DashboardEvent

    // System input, raised by the ViewModel from collected flows
    data class DashboardChanged(val data: DashboardData) : DashboardEvent
    data class PendingChargeChanged(val charge: PendingChargeUi?) : DashboardEvent
    data class ChargeStopChanged(val stop: ChargeStopUi?) : DashboardEvent
    data class ChargeStopAdded(val added: Boolean) : DashboardEvent
    data class EcoGuideChanged(val state: EcoGuideState) : DashboardEvent
    data class MonthChanged(val month: MonthSummary) : DashboardEvent
}

/** One-shot effects consumed exactly once by the route. */
sealed interface DashboardEffect {
    data class ShowMessage(val text: String) : DashboardEffect
    /** Switch to the ledger tab. */
    data object NavigateToLedger : DashboardEffect
    data class NavigateToCharge(val recordId: String) : DashboardEffect
}
