package com.example.pleos.domain.route

import com.example.pleos.domain.model.ChargerCandidate
import com.example.pleos.domain.model.NavEvent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** What the navigation is doing, folded from its events. */
data class RouteState(
    val active: Boolean = false,
    /** Increments on every new route, so a suggestion is made at most once per route. */
    val routeId: Int = 0,
    val destinationName: String? = null,
    val remainingKm: Double? = null,
) {
    fun reduce(event: NavEvent): RouteState = when (event) {
        is NavEvent.RouteStarted -> RouteState(
            active = true,
            routeId = routeId + 1,
            destinationName = event.destinationName,
            remainingKm = event.distanceKm,
        )
        // A route the app did not see start (it was set before the app ran) still counts.
        NavEvent.RoutingCompleted -> if (active) this else copy(active = true, routeId = routeId + 1)
        is NavEvent.TripInfo -> if (active) {
            copy(remainingKm = event.remainDistanceKm)
        } else {
            copy(active = true, routeId = routeId + 1, remainingKm = event.remainDistanceKm)
        }
        NavEvent.RouteCancelled, NavEvent.DestinationArrived -> copy(active = false, remainingKm = null)
        is NavEvent.DestinationChanged, is NavEvent.WaypointArrived, is NavEvent.Error -> this
    }
}

/** FEATURE_LOGIC L4-1 and L4-2 as pure functions. */
object ChargeStopRules {

    /** [기본값] Always suggest below this range, even on a short route. */
    const val MIN_RANGE_KM = 50.0

    /** [기본값] Range must cover the rest of the route with this margin. */
    const val ROUTE_MARGIN = 1.2

    /** [기본값] Search radius around the car. */
    const val SEARCH_RADIUS_KM = 10.0

    /** Candidates more than this many degrees off the heading are behind the car. */
    private const val AHEAD_DEGREES = 90.0

    /** Suggest only with an active route and a range that will not last it (L4-1). */
    fun shouldSuggest(rangeKm: Double?, route: RouteState): Boolean {
        if (!route.active || rangeKm == null) return false
        val needed = maxOf(MIN_RANGE_KM, (route.remainingKm ?: 0.0) * ROUTE_MARGIN)
        return rangeKm < needed
    }

    /**
     * Orders candidates the way L4-2 intends without a routing API: stations ahead of the car
     * within [SEARCH_RADIUS_KM] first, nearest first, so U-turn stations fall behind. When none
     * is ahead and close, the nearest overall is still offered rather than nothing.
     */
    fun rank(latitude: Double, longitude: Double, headingDeg: Double?, candidates: List<ChargerCandidate>): List<ChargerCandidate> {
        fun ahead(c: ChargerCandidate): Boolean {
            if (headingDeg == null) return true
            val bearing = bearingDeg(latitude, longitude, c.place.latitude, c.place.longitude)
            val diff = abs(((bearing - headingDeg) % 360 + 540) % 360 - 180)
            return diff <= AHEAD_DEGREES
        }
        val (preferred, rest) = candidates.partition { it.distanceKm <= SEARCH_RADIUS_KM && ahead(it) }
        return preferred.sortedBy { it.distanceKm } + rest.sortedBy { it.distanceKm }
    }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun bearingDeg(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLng = Math.toRadians(lng2 - lng1)
        val y = sin(dLng) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    private const val EARTH_RADIUS_KM = 6_371.0
}
