package com.example.pleos.domain.route

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.ChargerSource
import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.ChargerCandidate
import com.example.pleos.domain.model.WaypointSlot
import com.example.pleos.domain.usecase.estimateRangeKm
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A charging stop offered for the current route. */
data class ChargeStopSuggestion(
    val routeId: Int,
    val candidate: ChargerCandidate,
    val rangeKm: Double,
    val remainingKm: Double?,
    val destinationName: String?,
)

/**
 * FEATURE_LOGIC M4 for charging: when the range will not last the route, offers the best charger
 * ahead once per route and, if the driver accepts, adds it as the first navigation waypoint.
 * Runs for the life of the app process, like [com.example.pleos.domain.drive.VehicleMonitor].
 */
@Singleton
class ChargeStopAdvisor @Inject constructor(
    private val vehicle: VehicleGateway,
    private val car: CarPropertySource,
    private val navigation: NavigationGateway,
    private val chargers: ChargerSource,
) {
    private val route = MutableStateFlow(RouteState())

    /** Routes whose suggestion was shown and answered, or that had no candidate. */
    private val handled = MutableStateFlow(emptySet<Int>())

    private val _suggestion = MutableStateFlow<ChargeStopSuggestion?>(null)
    val suggestion: StateFlow<ChargeStopSuggestion?> = _suggestion.asStateFlow()

    suspend fun run() = coroutineScope {
        launch { navigation.events.collect { event -> route.update { it.reduce(event) } } }

        combine(vehicle.snapshot, car.properties) { s, p -> estimateRangeKm(s, p) }
            .combine(route) { range, r -> range to r }
            .combine(handled) { (range, r), done -> Triple(range, r, done) }
            .distinctUntilChanged()
            .collect { (range, r, done) ->
                val current = _suggestion.value
                when {
                    !r.active -> _suggestion.value = null
                    current != null && current.routeId == r.routeId ->
                        _suggestion.value = current.copy(rangeKm = range ?: current.rangeKm, remainingKm = r.remainingKm)
                    r.routeId !in done && ChargeStopRules.shouldSuggest(range, r) ->
                        _suggestion.value = findStop(r, range!!)
                }
            }
    }

    /** Adds the suggested charger as the first waypoint. Returns false when there is nothing to add. */
    suspend fun accept(): Boolean {
        val s = _suggestion.value ?: return false
        navigation.addWaypoint(s.candidate.place, WaypointSlot.FIRST)
        close(s.routeId)
        return true
    }

    fun dismiss() {
        _suggestion.value?.let { close(it.routeId) }
    }

    private fun close(routeId: Int) {
        handled.update { it + routeId }
        _suggestion.value = null
    }

    private suspend fun findStop(r: RouteState, rangeKm: Double): ChargeStopSuggestion? {
        val here = navigation.currentLocation()
        if (here == null) {
            handled.update { it + r.routeId }
            return null
        }
        val best = ChargeStopRules
            .rank(here.latitude, here.longitude, here.headingDeg, chargers.nearby(here.latitude, here.longitude, CANDIDATE_LIMIT))
            .firstOrNull()
        if (best == null) {
            handled.update { it + r.routeId }
            return null
        }
        return ChargeStopSuggestion(r.routeId, best, rangeKm, r.remainingKm, r.destinationName)
    }

    private companion object {
        const val CANDIDATE_LIMIT = 10
    }
}
