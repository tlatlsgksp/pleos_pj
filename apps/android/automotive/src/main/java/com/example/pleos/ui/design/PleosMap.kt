package com.example.pleos.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import com.naver.maps.map.compose.CameraPositionState
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import com.example.pleos.R
import com.example.pleos.domain.model.ReportType
import com.naver.maps.map.overlay.OverlayImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RoadReport
import com.example.pleos.ui.theme.Pleos
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapType
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PathOverlay
import com.naver.maps.map.compose.rememberCameraPositionState

/**
 * The one map the app draws on: the route as a blue path, reports as markers in their tone and
 * ride members as markers with their name. Native map chrome (zoom, compass, scale) stays off;
 * the car's own navigation owns those.
 */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PleosMap(
    modifier: Modifier = Modifier,
    route: List<LatLng> = emptyList(),
    reports: List<RoadReport> = emptyList(),
    members: List<Member> = emptyList(),
    speakerId: String? = null,
    /** Where to look when there is nothing else to frame. */
    fallback: LatLng = SEOUL,
    /** False for a map used as a picture (a card's art): no gestures, just the framed route. */
    interactive: Boolean = true,
    /** Pass one to drive the camera from outside ([MapControls]); otherwise the map keeps its own. */
    camera: CameraPositionState = rememberCameraPositionState { position = CameraPosition(fallback, FALLBACK_ZOOM) },
) {
    val c = Pleos.colors
    val framed = route + reports.map { LatLng(it.latitude, it.longitude) } + members.mapNotNull { it.position() }
    LaunchedEffect(framed) {
        if (framed.size >= 2) {
            camera.move(CameraUpdate.fitBounds(LatLngBounds.from(framed), FIT_PADDING_PX))
        } else if (framed.size == 1) {
            camera.move(CameraUpdate.scrollAndZoomTo(framed.first(), SINGLE_ZOOM))
        }
    }
    NaverMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(mapType = MapType.Navi, isNightModeEnabled = isSystemInDarkTheme()),
        uiSettings = MapUiSettings(
            isZoomControlEnabled = false,
            isCompassEnabled = false,
            isScaleBarEnabled = false,
            isLocationButtonEnabled = false,
            isIndoorLevelPickerEnabled = false,
            isLogoClickEnabled = false,
            isScrollGesturesEnabled = interactive,
            isZoomGesturesEnabled = interactive,
            isTiltGesturesEnabled = interactive,
            isRotateGesturesEnabled = interactive,
            isStopGesturesEnabled = interactive,
        ),
    ) {
        if (route.size >= 2) {
            PathOverlay(coords = route, width = ROUTE_WIDTH, color = c.signBlue, outlineWidth = ROUTE_OUTLINE, outlineColor = c.onSign)
        }
        reports.forEach { r ->
            Marker(
                state = MarkerState(LatLng(r.latitude, r.longitude)),
                icon = OverlayImage.fromResource(r.type.marker()),
                anchor = CENTER,
            )
        }
        members.forEach { m ->
            m.position()?.let { at ->
                val speaking = m.id == speakerId
                Marker(
                    state = MarkerState(at),
                    icon = OverlayImage.fromResource(if (speaking) R.drawable.marker_member_speaking else R.drawable.marker_member),
                    anchor = CENTER,
                    captionText = m.nickname,
                    captionTextSize = CAPTION_SIZE,
                    captionColor = c.textPrimary,
                    captionHaloColor = c.surface,
                    zIndex = if (speaking) 1 else 0,
                )
            }
        }
    }
}

/** The disc marker for a report, in its tone (res/drawable/marker_report_*). */
@DrawableRes
private fun ReportType.marker(): Int = when (this) {
    ReportType.ACCIDENT -> R.drawable.marker_report_accident
    ReportType.CONSTRUCTION -> R.drawable.marker_report_construction
    ReportType.CONGESTION -> R.drawable.marker_report_congestion
    ReportType.SPEED_CAMERA -> R.drawable.marker_report_camera
    ReportType.DISASTER -> R.drawable.marker_report_disaster
}

/** Discs sit on the point, not above it like a pin. */
private val CENTER = Offset(0.5f, 0.5f)

private fun Member.position(): LatLng? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return LatLng(lat, lng)
}

val SEOUL = LatLng(37.5665, 126.9780)

/**
 * Demo route Seoul → Gangneung Gyeongpo beach, a handful of points along the Yeongdong expressway.
 * Real routes come from the Naver Directions API over NaviHelper's route points.
 */
val DEMO_ROUTE_GANGNEUNG: List<LatLng> = listOf(
    SEOUL,
    LatLng(37.50, 127.25),
    LatLng(37.43, 127.60),
    LatLng(37.47, 127.95),
    LatLng(37.58, 128.14),
    LatLng(37.62, 128.33),
    LatLng(37.66, 128.52),
    LatLng(37.71, 128.72),
    LatLng(37.80, 128.90),
)

const val FALLBACK_ZOOM = 11.0
private const val SINGLE_ZOOM = 13.0
/** Pixels; the display is 160dpi so 1dp = 1px. */
private const val FIT_PADDING_PX = 160
private val ROUTE_WIDTH = 12.dp
private val ROUTE_OUTLINE = 2.dp
private val CAPTION_SIZE = 20.sp

/**
 * Zoom in, zoom out and back to the car, stacked on the driver's side of a map. The car's own
 * navigation has these too; a map without them cannot be read at the scale the driver wants.
 */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MapControls(camera: CameraPositionState, me: Member?, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PleosIconButton(R.drawable.ic_add, stringResource(R.string.map_zoom_in), { scope.launch { camera.animate(CameraUpdate.zoomIn()) } })
        PleosIconButton(R.drawable.ic_remove, stringResource(R.string.map_zoom_out), { scope.launch { camera.animate(CameraUpdate.zoomOut()) } })
        me?.position()?.let { at ->
            PleosIconButton(R.drawable.ic_my_location, stringResource(R.string.map_my_location), { scope.launch { camera.animate(CameraUpdate.scrollAndZoomTo(at, SINGLE_ZOOM)) } })
        }
    }
}
