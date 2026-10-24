package com.example.pleos.domain.model

data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val poiId: String? = null,
)

/** NaviHelper allows at most two waypoints. */
enum class WaypointSlot { FIRST, SECOND }

/** Navigation events, mapped from NaviHelperEventListener callbacks. */
sealed interface NavEvent {
    data object RouteStarted : NavEvent
    data object RoutingCompleted : NavEvent
    data object RouteCancelled : NavEvent
    data class DestinationChanged(val destination: Place?) : NavEvent
    data object DestinationArrived : NavEvent
    data class WaypointArrived(val slot: WaypointSlot) : NavEvent
    data class TripInfo(val remainDistanceKm: Double, val remainTimeSec: Long) : NavEvent
    data class Error(val code: String) : NavEvent
}
