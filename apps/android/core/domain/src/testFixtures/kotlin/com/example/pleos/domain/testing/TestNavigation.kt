package com.example.pleos.domain.testing

import com.example.pleos.domain.gateway.ChargerSource
import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.model.ChargerCandidate
import com.example.pleos.domain.model.LocationFix
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.WaypointSlot
import com.example.pleos.domain.route.ChargeStopRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** A navigation the test drives with [emit]; records the waypoints it was asked to add. */
class RecordingNavigationGateway(
    var location: LocationFix? = LocationFix(37.5547, 126.9707, headingDeg = 160.0, timestampMillis = 0),
) : NavigationGateway {
    private val flow = MutableSharedFlow<NavEvent>(extraBufferCapacity = 16)
    override val events: Flow<NavEvent> = flow
    val waypoints = mutableListOf<Pair<Place, WaypointSlot>>()

    suspend fun emit(event: NavEvent) = flow.emit(event)

    override suspend fun setDestination(place: Place) = Unit
    override suspend fun addWaypoint(place: Place, slot: WaypointSlot) {
        waypoints += place to slot
    }
    override suspend fun removeWaypoint(slot: WaypointSlot) = Unit
    override suspend fun cancelRoute() = Unit
    override suspend fun currentLocation(): LocationFix? = location
}

/** Returns [places] with distances measured from the query point. */
class ListChargerSource(private val places: List<Place>, private val isDemo: Boolean = true) : ChargerSource {
    override suspend fun nearby(latitude: Double, longitude: Double, limit: Int): List<ChargerCandidate> =
        places.map { ChargerCandidate(it, ChargeStopRules.distanceKm(latitude, longitude, it.latitude, it.longitude), isDemo) }
            .sortedBy { it.distanceKm }
            .take(limit)
}
