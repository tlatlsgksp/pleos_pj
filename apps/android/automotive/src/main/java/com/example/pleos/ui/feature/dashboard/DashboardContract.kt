package com.example.pleos.ui.feature.dashboard

import com.example.pleos.domain.eco.EcoGuideState
import com.example.pleos.domain.model.DashboardData
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.ReportType
import com.example.pleos.domain.model.RideMembership
import com.example.pleos.domain.model.Riding
import com.example.pleos.domain.model.RoadReport
import com.example.pleos.domain.repository.SettingsRepository

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
    /** The bundled list is not real stations; the screen says so. */
    val isDemoData: Boolean,
    val brand: String? = null,
    /** One of the driver's card brands. */
    val isPreferredBrand: Boolean = false,
)

/**
 * What the group riding card on home shows, by priority: in a ride > invited > left but still open
 * > nothing (docs/visual/wireframes.html E1).
 */
sealed interface RideCardUi {
    data object None : RideCardUi
    data class Invited(val riding: Riding, val count: Int) : RideCardUi
    data class In(val riding: Riding) : RideCardUi
    data class Left(val riding: Riding) : RideCardUi
}

internal fun rideCard(ridings: List<Riding>): RideCardUi {
    ridings.firstOrNull { it.membership == RideMembership.IN }?.let { return RideCardUi.In(it) }
    val invites = ridings.filter { it.membership == RideMembership.INVITED }
    if (invites.isNotEmpty()) return RideCardUi.Invited(invites.first(), invites.size)
    ridings.firstOrNull { it.membership == RideMembership.LEFT }?.let { return RideCardUi.Left(it) }
    return RideCardUi.None
}

/** The banner that drops from the top, most urgent first. One at a time. */
sealed interface HomeBanner {
    data object Airbag : HomeBanner
    data class RouteReport(val report: RoadReport) : HomeBanner
}

/** Immutable home state. null = unknown or unsupported on this vehicle. */
data class DashboardState(
    val me: Member? = null,
    val myKmPerUnit: Double? = null,
    val myRank: Int? = null,
    val rideCard: RideCardUi = RideCardUi.None,
    val reportsOnRoute: List<RoadReport> = emptyList(),
    /** The report that just appeared on the route; shown in the banner until it times out. */
    val freshReport: RoadReport? = null,
    val chargeStop: ChargeStopUi? = null,
    val isAddingStop: Boolean = false,
    val ecoGuide: EcoGuideState = EcoGuideState(),
    val ambientColor: Int = SettingsRepository.DEFAULT_AMBIENT_COLOR,
    /** The paint saved in the skin studio. */
    val bodyPaint: Int = SettingsRepository.DEFAULT_BODY_PAINT,
    val airbagDeployed: Boolean = false,
    val pendingCharge: PendingChargeUi? = null,
    /** True for a few seconds after a new detected charge, so the banner drops once and goes. */
    val pendingFresh: Boolean = false,
    val reportSheetOpen: Boolean = false,
    val ambientPickerOpen: Boolean = false,
) {
    val banner: HomeBanner?
        get() = when {
            airbagDeployed -> HomeBanner.Airbag
            freshReport != null -> HomeBanner.RouteReport(freshReport)
            else -> null
        }
}

sealed interface DashboardEvent {
    // User input
    data object ReportTapped : DashboardEvent
    data class ReportPicked(val type: ReportType) : DashboardEvent
    data object ReportSheetDismissed : DashboardEvent
    data object ChargerTapped : DashboardEvent
    data object EcoGuideToggleTapped : DashboardEvent
    data object AmbientTapped : DashboardEvent
    data class AmbientPicked(val rgb: Int) : DashboardEvent
    data object AmbientPickerDismissed : DashboardEvent
    data object RideCardTapped : DashboardEvent
    data class JoinRideTapped(val ridingId: String) : DashboardEvent
    data class LeaveRideTapped(val ridingId: String) : DashboardEvent
    data class DeclineRideTapped(val ridingId: String) : DashboardEvent
    data object OpenChargeTapped : DashboardEvent
    data object BannerTapped : DashboardEvent

    // System input, raised by the ViewModel from collected flows
    data class DashboardChanged(val data: DashboardData) : DashboardEvent
    data class PendingChargeChanged(val charge: PendingChargeUi?) : DashboardEvent
    data class ChargeStopChanged(val stop: ChargeStopUi?) : DashboardEvent
    data class ChargeStopAdded(val added: Boolean) : DashboardEvent
    data class EcoGuideChanged(val state: EcoGuideState) : DashboardEvent
    data class CommunityChanged(val me: Member, val ridings: List<Riding>, val myKmPerUnit: Double?, val myRank: Int?) : DashboardEvent
    data class ReportsChanged(val reports: List<RoadReport>) : DashboardEvent
    data class AmbientChanged(val rgb: Int) : DashboardEvent
    data class BodyPaintChanged(val rgb: Int) : DashboardEvent
    data object FreshReportExpired : DashboardEvent
    data object PendingBannerExpired : DashboardEvent
}

/** One-shot effects consumed exactly once by the route. */
sealed interface DashboardEffect {
    data class ShowMessage(val text: String) : DashboardEffect
    data class NavigateToCharge(val recordId: String) : DashboardEffect
    /** Open group riding: the room when [ridingId] is set, the ride list otherwise. */
    data class OpenRiding(val ridingId: String?) : DashboardEffect
    /** Show where the report is on the map tab. */
    data object OpenMap : DashboardEffect
}

/** Mood light colors offered on home; the skin studio has the full picker. */
val AMBIENT_PRESETS = listOf(0xFFFFFF, 0x9FD3F5, 0x5FA8E3, 0x7CE0B4, 0xFFD27A, 0xFF9E7A, 0xD7A6F5, 0xF5A6C8)
