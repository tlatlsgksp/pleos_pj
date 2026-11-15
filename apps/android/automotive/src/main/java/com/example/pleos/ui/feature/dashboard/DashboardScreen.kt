package com.example.pleos.ui.feature.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.pleos.R
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.ReportType
import com.example.pleos.domain.model.RideMembership
import com.example.pleos.domain.model.Riding
import com.example.pleos.domain.model.RoadReport
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.AdaptiveScreen
import com.example.pleos.ui.design.BannerButton
import com.example.pleos.ui.design.ButtonSize
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.CarModel
import com.example.pleos.ui.design.ColorSwatch
import com.example.pleos.ui.design.DEMO_ROUTE_GANGNEUNG
import com.example.pleos.ui.design.BigSwatch
import com.example.pleos.ui.design.BottomSheet
import com.example.pleos.ui.design.ReportSign
import com.example.pleos.ui.design.icon
import com.example.pleos.ui.design.reportTone
import com.example.pleos.ui.design.LiveBadge
import com.example.pleos.ui.design.liveOutline
import com.example.pleos.ui.design.PleosMap
import com.example.pleos.ui.design.ROW_RADIUS
import com.example.pleos.ui.design.HomeCard
import com.example.pleos.ui.design.ModalChoice
import com.example.pleos.ui.design.ImageCard
import com.example.pleos.ui.design.MemberAvatar
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.QuickAction
import com.example.pleos.ui.design.RoadSign
import com.example.pleos.ui.design.SignArrow
import com.example.pleos.ui.design.arrowToward
import com.example.pleos.ui.design.destinationArrow
import com.example.pleos.ui.design.SignKind
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.TopBanner
import com.example.pleos.ui.design.WindowWidth
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme
import java.time.Instant

/**
 * Home (.impeccable/surfaces/apps-android-automotive.md, FIRST VIEWPORT). Row one: my car and the
 * group ride. Row two: four quick actions that finish in place. Road signs only for what lies on
 * the route. The floating menu sits over this screen and takes no space from it.
 */
@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Pleos.colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AdaptiveScreen(Modifier.padding(vertical = Space.l)) { window ->
                if (window.width == WindowWidth.TWO) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        Row(Modifier.weight(1.25f), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            MyCarCard(state, Modifier.weight(1f).fillMaxSize())
                            RideCard(state.rideCard, state.me, onEvent, Modifier.weight(1f).fillMaxSize())
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            QuickActions(state, onEvent, Modifier.weight(1f).fillMaxSize())
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        MyCarCard(state, Modifier.weight(1.1f).fillMaxWidth())
                        RideCard(state.rideCard, state.me, onEvent, Modifier.weight(1f).fillMaxWidth())
                        Row(Modifier.weight(0.9f), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            QuickActions(state, onEvent, Modifier.weight(1f).fillMaxSize(), compact = true)
                        }
                    }
                }
            }
            if (state.reportSheetOpen) ReportSheet(state.me, onEvent)
            if (state.ambientPickerOpen) AmbientPicker(state.ambientColor, onEvent)
            HomeBannerHost(state.banner, state.me, onEvent)
        }
    }
}

@Composable
private fun MyCarCard(state: DashboardState, modifier: Modifier) {
    val c = Pleos.colors
    HomeCard(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CarModel(paint = state.bodyPaint, ambient = state.ambientColor, modifier = Modifier.fillMaxSize())
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Column(Modifier.weight(1f)) {
                Text(state.me?.nickname.orEmpty(), style = PleosText.sectionTitle, color = c.textPrimary, maxLines = 1)
                Text(state.me?.carModel.orEmpty(), style = PleosText.body, color = c.textSecondary, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                state.myKmPerUnit?.let {
                    Text(Format.efficiency(it, state.me?.isEv ?: true), style = PleosText.figure, color = c.textPrimary)
                }
                state.myRank?.let { Text(stringResource(R.string.home_week_efficiency, it), style = PleosText.label, color = c.textSecondary) }
            }
        }
    }
}

/**
 * The group ride on home, one look per state: an invitation, a ride I am in (green, members large),
 * a ride I left that is still going (picture dimmed), or nothing yet. Actions sit left.
 */
@Composable
private fun RideCard(card: RideCardUi, me: Member?, onEvent: (DashboardEvent) -> Unit, modifier: Modifier) {
    AnimatedContent(card, modifier, contentKey = { it::class }, transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) }, label = "rideCard") { card ->
        val c = Pleos.colors
        val live = card is RideCardUi.In
        val riding = when (card) {
            is RideCardUi.In -> card.riding
            is RideCardUi.Invited -> card.riding
            is RideCardUi.Left -> card.riding
            RideCardUi.None -> null
        }
        ImageCard(
            R.drawable.home_art_ride,
            Modifier.fillMaxSize().liveOutline(live),
            onClick = { onEvent(DashboardEvent.RideCardTapped) },
            imageAlpha = if (card is RideCardUi.Left) 0.4f else 1f,
            // A ride shows its route; with no ride, the picture.
            art = riding?.let { r -> { PleosMap(Modifier.fillMaxSize(), route = DEMO_ROUTE_GANGNEUNG, members = r.joined, interactive = false) } },
            overlay = if (riding != null) ({
                Row(Modifier.align(Alignment.TopStart), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    if (live) LiveBadge(stringResource(R.string.ride_tag_in))
                    riding.destinationName?.let { RoadSign(it, SignKind.DESTINATION, arrow = riding.destinationArrow(me)) }
                }
            }) else null,
        ) {
            Column {
                Text(
                    when (card) {
                        is RideCardUi.Invited -> if (card.count > 1) stringResource(R.string.ride_invites, card.count) else card.riding.name
                        RideCardUi.None -> stringResource(R.string.ride_title)
                        else -> riding!!.name
                    },
                    style = PleosText.cardTitle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when (card) {
                        RideCardUi.None -> stringResource(R.string.ride_none)
                        is RideCardUi.Invited -> stringResource(R.string.ride_invited, card.riding.host.nickname, card.riding.joined.size)
                        is RideCardUi.In -> stringResource(R.string.ride_joined_count, card.riding.joined.size)
                        is RideCardUi.Left -> stringResource(R.string.ride_left, card.riding.joined.size)
                    },
                    style = PleosText.body, color = if (live) c.signGreen else c.textSecondary, maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                when (card) {
                    RideCardUi.None -> PleosButton(stringResource(R.string.ride_create), { onEvent(DashboardEvent.RideCardTapped) }, style = ButtonStyle.Filled)
                    is RideCardUi.Invited -> if (card.count > 1) {
                        PleosButton(stringResource(R.string.ride_invites_open), { onEvent(DashboardEvent.RideCardTapped) }, style = ButtonStyle.Filled)
                    } else {
                        PleosButton(stringResource(R.string.ride_join), { onEvent(DashboardEvent.JoinRideTapped(card.riding.id)) }, style = ButtonStyle.Filled)
                        PleosButton(stringResource(R.string.ride_decline), { onEvent(DashboardEvent.DeclineRideTapped(card.riding.id)) })
                    }
                    is RideCardUi.In -> {
                        PleosButton(stringResource(R.string.ride_open), { onEvent(DashboardEvent.RideCardTapped) }, style = ButtonStyle.Filled)
                        PleosButton(stringResource(R.string.ride_leave), { onEvent(DashboardEvent.LeaveRideTapped(card.riding.id)) })
                    }
                    is RideCardUi.Left -> PleosButton(stringResource(R.string.ride_rejoin), { onEvent(DashboardEvent.JoinRideTapped(card.riding.id)) }, style = ButtonStyle.Filled)
                }
                // Members on the right; large while I am in the ride.
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End)) {
                    riding?.joined?.take(4)?.forEach { m -> MemberAvatar(m.nickname, m.handle, large = live, dim = card is RideCardUi.Left) }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(state: DashboardState, onEvent: (DashboardEvent) -> Unit, itemModifier: Modifier, compact: Boolean = false) {
    val c = Pleos.colors
    val ahead = state.reportsOnRoute.filter { it.aheadKm != null }
    QuickAction(
        title = stringResource(R.string.qa_report),
        detail = if (ahead.isEmpty()) stringResource(R.string.qa_report_types) else stringResource(R.string.qa_report_ahead, ahead.size),
        image = R.drawable.home_art_report,
        onClick = { onEvent(DashboardEvent.ReportTapped) },
        modifier = itemModifier,
        sign = ahead.firstOrNull()?.let { r -> { ReportSign(r.type, "${r.type.label()} ${Format.distance(r.aheadKm!!)}", arrow = arrowToward(state.me, r.latitude, r.longitude)) } },
    )
    QuickAction(
        title = stringResource(R.string.qa_charger),
        detail = if (state.chargeStop != null) stringResource(R.string.qa_charger_add) else stringResource(R.string.qa_charger_none),
        image = R.drawable.home_art_charger,
        onClick = { onEvent(DashboardEvent.ChargerTapped) },
        modifier = itemModifier,
        enabled = !state.isAddingStop,
        sign = state.chargeStop?.let { stop -> { RoadSign(stop.brand ?: stop.name, SignKind.WAYPOINT, arrow = SignArrow.RIGHT) } },
    )
    if (!compact) {
        QuickAction(
            title = stringResource(R.string.qa_eco),
            detail = stringResource(if (state.ecoGuide.enabled) R.string.qa_on else R.string.qa_off),
            image = R.drawable.home_art_eco,
            onClick = { onEvent(DashboardEvent.EcoGuideToggleTapped) },
            modifier = itemModifier,
        )
        QuickAction(
            title = stringResource(R.string.qa_light),
            detail = stringResource(R.string.qa_light_pick),
            image = R.drawable.home_art_light,
            onClick = { onEvent(DashboardEvent.AmbientTapped) },
            modifier = itemModifier,
            trailing = { ColorSwatch(state.ambientColor) },
        )
    }
}

@Composable
private fun ReportSheet(me: Member?, onEvent: (DashboardEvent) -> Unit) {
    BottomSheet(stringResource(R.string.report_sheet_title), onDismiss = { onEvent(DashboardEvent.ReportSheetDismissed) }) {
        // Where the report will land: my position on the map.
        PleosMap(Modifier.fillMaxWidth().height(Space.xl * 5).clip(RoundedCornerShape(ROW_RADIUS)), members = listOfNotNull(me))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(ReportType.ACCIDENT, ReportType.CONSTRUCTION, ReportType.CONGESTION, ReportType.SPEED_CAMERA, ReportType.DISASTER).forEach { type ->
                val tone = reportTone(type)
                ModalChoice(type.label(), type.icon(), { onEvent(DashboardEvent.ReportPicked(type)) }, Modifier.weight(1f), plate = tone.fill, ink = tone.ink)
            }
        }
    }
}

@Composable
private fun AmbientPicker(current: Int, onEvent: (DashboardEvent) -> Unit) {
    val c = Pleos.colors
    BottomSheet(stringResource(R.string.ambient_title), onDismiss = { onEvent(DashboardEvent.AmbientPickerDismissed) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            AMBIENT_PRESETS.forEach { rgb -> BigSwatch(rgb, rgb == current, { onEvent(DashboardEvent.AmbientPicked(rgb)) }) }
        }
        Text(stringResource(R.string.ambient_hint), style = PleosText.body, color = c.textSecondary)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HomeBannerHost(banner: HomeBanner?, me: Member?, onEvent: (DashboardEvent) -> Unit) {
    val title = when (banner) {
        HomeBanner.Airbag -> stringResource(R.string.banner_airbag)
        is HomeBanner.RouteReport -> stringResource(R.string.banner_route_report, banner.report.type.label())
        null -> ""
    }
    TopBanner(
        visible = banner != null,
        title = title,
        onClick = { onEvent(DashboardEvent.BannerTapped) },
        leading = {
            if (banner is HomeBanner.RouteReport) ReportSign(banner.report.type, banner.report.type.label(), arrow = arrowToward(me, banner.report.latitude, banner.report.longitude))
        },
    ) {
    }
}

private val previewMe = Member("me", "민수", "minsu_ioniq", "아이오닉 5", isEv = true)
private val previewRide = Riding(
    "r1", "도윤의 라이딩", Member("n5", "도윤", "doyun", "아이오닉 5", true), "강릉 경포해변",
    joined = listOf(Member("n5", "도윤", "doyun", "아이오닉 5", true), Member("n1", "지은", "jieun", "EV6", true), Member("n2", "현우", "hw", "아이오닉 5", true)),
    away = emptyList(), membership = RideMembership.INVITED,
)

@Preview(name = "Home, two thirds", showBackground = true, widthDp = 1700, heightDp = 1100)
@Composable
private fun HomePreview() {
    PleosTheme {
        DashboardScreen(
            DashboardState(
                me = previewMe, myKmPerUnit = 6.0, myRank = 214, rideCard = RideCardUi.Invited(previewRide, 1),
                reportsOnRoute = listOf(RoadReport("p1", ReportType.ACCIDENT, Instant.now(), 0.0, 0.0, 2.4)),
                chargeStop = ChargeStopUi("E-pit 강릉", 1.2, 90.0, 160.0, "강릉", false, "E-pit", true),
            ),
            {},
            SnackbarHostState(),
        )
    }
}
