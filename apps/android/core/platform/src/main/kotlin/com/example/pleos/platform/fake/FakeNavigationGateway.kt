package com.example.pleos.platform.fake

import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.model.LocationFix
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.WaypointSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [NavigationGateway] for the demo mode and tests. The car sits near Seoul Station facing south. */
class FakeNavigationGateway(
    var location: LocationFix? = LocationFix(37.5547, 126.9707, headingDeg = 160.0, timestampMillis = 0),
) : NavigationGateway {

    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 64)
    override val events: Flow<NavEvent> = _events

    private val _waypoints = MutableStateFlow<Map<WaypointSlot, Place>>(emptyMap())
    val waypoints: StateFlow<Map<WaypointSlot, Place>> = _waypoints.asStateFlow()

    fun emit(event: NavEvent) {
        _events.tryEmit(event)
    }

    /** Starts a route to [name] that is [km] long. */
    fun startRoute(name: String, km: Double) {
        _waypoints.value = emptyMap()
        emit(NavEvent.RouteStarted(name, km, null))
        emit(NavEvent.TripInfo(km, (km * 60).toLong()))
    }

    override suspend fun setDestination(place: Place) = startRoute(place.name, 0.0)

    override suspend fun addWaypoint(place: Place, slot: WaypointSlot) {
        _waypoints.update { it + (slot to place) }
    }

    override suspend fun removeWaypoint(slot: WaypointSlot) {
        _waypoints.update { it - slot }
    }

    override suspend fun cancelRoute() {
        _waypoints.value = emptyMap()
        emit(NavEvent.RouteCancelled)
    }

    override suspend fun currentLocation(): LocationFix? = location
}
