package com.example.pleos.ui.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.pleos.R
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RoadReport
import com.example.pleos.domain.repository.CommunityRepository
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.DEMO_ROUTE_GANGNEUNG
import com.example.pleos.ui.design.HomeCard
import com.example.pleos.ui.design.FALLBACK_ZOOM
import com.example.pleos.ui.design.MapControls
import com.example.pleos.ui.design.PleosMap
import com.example.pleos.ui.design.SEOUL
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.rememberCameraPositionState
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.ReportSign
import com.example.pleos.ui.design.SignArrow
import com.example.pleos.ui.design.arrowToward
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.feature.dashboard.label
import com.example.pleos.ui.theme.Pleos
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapState(
    val me: Member? = null,
    /** Everything reported around the car, newest first. */
    val nearby: List<RoadReport> = emptyList(),
    /** The subset on the active route, nearest first; empty without a route. */
    val onRoute: List<RoadReport> = emptyList(),
) {
    val routeActive: Boolean get() = onRoute.isNotEmpty()
}

@HiltViewModel
class MapViewModel @Inject constructor(community: CommunityRepository) : ViewModel() {
    val state: StateFlow<MapState> = combine(community.me, community.reportsNearby, community.reportsOnRoute) { me, nearby, onRoute ->
        // Around the car means within reach, closest first; without a position everything counts.
        val around = nearby
            .filter { r -> me.distanceKmTo(r)?.let { it <= NEARBY_KM } ?: true }
            .sortedBy { r -> me.distanceKmTo(r) ?: Double.MAX_VALUE }
        MapState(me, around, onRoute)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState())
}

/**
 * The map tab: the Naver map around the car with every nearby report, plus the route and its
 * reports once the navigation is guiding. The card on the driver's side lists what is on the
 * route when there is one, otherwise what is around.
 */
@Composable
fun MapRoute(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Pleos.colors
    val camera = rememberCameraPositionState { position = CameraPosition(SEOUL, FALLBACK_ZOOM) }
    Box(Modifier.fillMaxSize()) {
        PleosMap(
            modifier = Modifier.fillMaxSize(),
            route = if (state.routeActive) DEMO_ROUTE_GANGNEUNG else emptyList(),
            reports = state.nearby,
            members = listOfNotNull(state.me),
            camera = camera,
        )
        MapControls(camera, state.me, Modifier.align(Alignment.BottomStart).padding(Space.l))
        HomeCard(Modifier.align(Alignment.TopStart).padding(Space.l).width(Space.xl * 10)) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                if (state.routeActive) {
                    Text(stringResource(R.string.map_on_route), style = PleosText.cardTitle, color = c.textPrimary)
                    state.onRoute.forEach { r -> ReportRow(r, r.aheadKm?.let(Format::distance), arrowToward(state.me, r.latitude, r.longitude)) }
                } else {
                    Text(stringResource(R.string.map_nearby), style = PleosText.cardTitle, color = c.textPrimary)
                    if (state.nearby.isEmpty()) {
                        Text(stringResource(R.string.map_nearby_empty), style = PleosText.body, color = c.textSecondary)
                    }
                    state.nearby.forEach { r ->
                        ReportRow(r, state.me?.distanceKmTo(r)?.let(Format::distance), arrowToward(state.me, r.latitude, r.longitude))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(r: RoadReport, distance: String?, arrow: SignArrow) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        ReportSign(r.type, r.type.label(), arrow = arrow)
        distance?.let { Text(it, style = PleosText.itemTitle, color = Pleos.colors.textPrimary) }
    }
}

/** Great-circle distance from the member's last position; null when it is not shared. */
internal fun Member.distanceKmTo(r: RoadReport): Double? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val dLat = Math.toRadians(r.latitude - lat)
    val dLng = Math.toRadians(r.longitude - lng)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(r.latitude)) * sin(dLng / 2) * sin(dLng / 2)
    return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private const val EARTH_RADIUS_KM = 6371.0
private const val NEARBY_KM = 20.0
