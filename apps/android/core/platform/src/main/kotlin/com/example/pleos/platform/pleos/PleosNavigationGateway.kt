package com.example.pleos.platform.pleos

import android.content.Context
import android.content.Intent
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.model.LocationFix
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.WaypointSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [NavigationGateway] on the Pleos NaviHelper SDK 3.0.5. Connects once for the app process.
 * NaviHelper answers queries through the listener, so [currentLocation] asks and waits for the
 * matching callback. Distances from the SDK are taken as metres; raw values are logged under
 * [TAG] so the unit can be checked on the emulator.
 */
internal class PleosNavigationGateway(context: Context) : NavigationGateway {

    private val navi = NaviHelper(context.applicationContext)
    private val connected = CompletableDeferred<Boolean>()
    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 64)
    private val location = MutableStateFlow<LocationFix?>(null)

    override val events: Flow<NavEvent> = _events.asSharedFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) {
            Log.d(TAG, "routeStarted $info")
            _events.tryEmit(NavEvent.RouteStarted(info.name, info.distance / 1000.0, info.tollFare.toLong()))
        }

        override fun onRoutingCompleted() {
            _events.tryEmit(NavEvent.RoutingCompleted)
        }

        override fun onRouteCancelled() {
            _events.tryEmit(NavEvent.RouteCancelled)
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            _events.tryEmit(NavEvent.DestinationArrived)
        }

        override fun onWayPointArrived(info: WaypointArrivedInfo) {
            val slot = if (info.index == WaypointIndex.SECOND) WaypointSlot.SECOND else WaypointSlot.FIRST
            _events.tryEmit(NavEvent.WaypointArrived(slot))
        }

        override fun onTripInfo(info: TripInfo) {
            Log.d(TAG, "tripInfo $info")
            _events.tryEmit(NavEvent.TripInfo(info.distance / 1000.0, info.eta.toLong()))
        }

        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            location.value = LocationFix(info.latitude, info.longitude, info.heading, System.currentTimeMillis())
        }

        override fun onError(code: NaviErrorCode) {
            Log.w(TAG, "error $code")
            _events.tryEmit(NavEvent.Error(code.name))
        }
    }

    init {
        navi.initialize(
            {
                navi.addListener(listener)
                connected.complete(true)
            },
            { error ->
                Log.w(TAG, "NaviHelper connection failed", error)
                connected.complete(false)
            },
        )
    }

    private suspend fun ready(): Boolean = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connected.await() } == true

    override suspend fun setDestination(place: Place) {
        if (!ready()) return
        navi.requestRoute(
            RouteInfo(
                place.latitude, place.longitude, null, null,
                place.name, place.poiId.orEmpty(), "", place.address.orEmpty(),
                RouteOption.RECOMMENDED, "", false,
            ),
        )
    }

    override suspend fun addWaypoint(place: Place, slot: WaypointSlot) {
        if (!ready()) return
        val index = if (slot == WaypointSlot.SECOND) WaypointIndex.SECOND else WaypointIndex.FIRST
        navi.addWaypoint(
            RequestWaypointInfo(place.latitude, place.longitude, place.name, index, place.poiId.orEmpty(), place.address.orEmpty(), "", ""),
        )
    }

    override suspend fun removeWaypoint(slot: WaypointSlot) {
        if (!ready()) return
        navi.removeWaypoint(if (slot == WaypointSlot.SECOND) WaypointIndex.SECOND else WaypointIndex.FIRST)
    }

    override suspend fun cancelRoute() {
        if (ready()) navi.cancelRoute()
    }

    override suspend fun currentLocation(): LocationFix? {
        if (!ready()) return null
        val askedAt = System.currentTimeMillis()
        navi.getCurrentLocationInfo()
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            location.filter { it != null && it.timestampMillis >= askedAt }.first()
        } ?: location.value
    }

    companion object {
        private const val TAG = "PleosNavi"
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val LOCATION_TIMEOUT_MS = 3_000L
        private const val NAVI_ACTION = "ai.pleos.playground.intent.action.NAVI_START"

        /** True when the Pleos navigation service is installed. */
        fun isAvailable(context: Context): Boolean {
            val pm = context.packageManager
            val intent = Intent(NAVI_ACTION)
            return pm.queryIntentServices(intent, 0).isNotEmpty() || pm.queryIntentActivities(intent, 0).isNotEmpty()
        }
    }
}
