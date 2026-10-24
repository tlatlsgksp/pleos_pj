package com.example.pleos.domain.gateway

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
}
