package com.example.pleos.domain.gateway

import com.example.pleos.domain.model.ChargerCandidate
import com.example.pleos.domain.model.LocationFix
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.WaypointSlot
import kotlinx.coroutines.flow.Flow

/** Route control and navigation events via the NaviHelper SDK. Results arrive as [events], not return values. */
interface NavigationGateway {
    val events: Flow<NavEvent>

    suspend fun setDestination(place: Place)

    suspend fun addWaypoint(place: Place, slot: WaypointSlot)

    suspend fun removeWaypoint(slot: WaypointSlot)

    suspend fun cancelRoute()

    /** The car's position as the navigation knows it, or null when it does not answer. */
    suspend fun currentLocation(): LocationFix?
}

/**
 * Charging stations near a point. NaviHelper has no POI search, so this comes from elsewhere:
 * a bundled demo list for now, the backend's charger API cache later (FEATURE_LOGIC L4-2).
 */
interface ChargerSource {
    suspend fun nearby(latitude: Double, longitude: Double, limit: Int): List<ChargerCandidate>
}
