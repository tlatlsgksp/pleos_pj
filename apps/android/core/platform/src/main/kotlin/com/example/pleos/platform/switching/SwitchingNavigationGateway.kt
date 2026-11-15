package com.example.pleos.platform.switching

import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.LocationFix
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.WaypointSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Same switching rule as [SwitchingVehicleGateway], for navigation. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwitchingNavigationGateway(
    requestedMode: Flow<DataSourceMode>,
    private val real: NavigationGateway?,
    private val fake: NavigationGateway,
) : NavigationGateway {

    private val active: Flow<NavigationGateway> = requestedMode
        .map { if (it == DataSourceMode.REAL && real != null) real else fake }
        .distinctUntilChanged()

    override val events: Flow<NavEvent> = active.flatMapLatest { it.events }

    override suspend fun setDestination(place: Place) = active.first().setDestination(place)

    override suspend fun addWaypoint(place: Place, slot: WaypointSlot) = active.first().addWaypoint(place, slot)

    override suspend fun removeWaypoint(slot: WaypointSlot) = active.first().removeWaypoint(slot)

    override suspend fun cancelRoute() = active.first().cancelRoute()

    override suspend fun currentLocation(): LocationFix? = active.first().currentLocation()
}
