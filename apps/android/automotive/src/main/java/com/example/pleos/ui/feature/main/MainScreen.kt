package com.example.pleos.ui.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.pleos.R
import com.example.pleos.ui.design.NavCard
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.TWO_COLUMN_MIN_WIDTH_DP
import com.example.pleos.ui.design.MenuItem
import com.example.pleos.ui.feature.dashboard.DashboardRoute
import com.example.pleos.ui.feature.map.MapRoute
import com.example.pleos.ui.feature.mycar.MyCarGroup
import com.example.pleos.ui.feature.mycar.MyCarRoute
import com.example.pleos.ui.feature.ranking.RankingRoute
import com.example.pleos.ui.theme.Pleos

/** Top-level destinations, in menu order. */
enum class MainTab { HOME, RANKING, MAP, ME, SETTINGS }

/**
 * The app's shell: the navigation is a card of its own, a rail on the left in the two-thirds window
 * and a row at the bottom in the one-third window; each destination fills the rest.
 */
@Composable
fun MainRoute(
    onOpenCharge: (recordId: String) -> Unit,
    onOpenSkin: () -> Unit,
    onOpenRiding: (ridingId: String?) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    val items = listOf(
        MenuItem(stringResource(R.string.menu_home), R.drawable.ic_home),
        MenuItem(stringResource(R.string.menu_ranking), R.drawable.ic_rank),
        MenuItem(stringResource(R.string.menu_map), R.drawable.ic_map),
        MenuItem(stringResource(R.string.menu_me), R.drawable.ic_person),
        MenuItem(stringResource(R.string.menu_settings), R.drawable.ic_settings),
    )
    val content: @Composable () -> Unit = {
        // Destinations cross-fade with a short lift, so a tap on the menu reads as a page change.
        AnimatedContent(
            targetState = MainTab.entries[tab],
            transitionSpec = {
                (fadeIn(tween(TAB_FADE_MS)) + slideInVertically(tween(TAB_FADE_MS)) { it / TAB_LIFT_FRACTION })
                    .togetherWith(fadeOut(tween(TAB_FADE_MS / 2)) + slideOutVertically(tween(TAB_FADE_MS / 2)) { -it / TAB_LIFT_FRACTION })
            },
            label = "tab",
        ) { current ->
            when (current) {
                MainTab.HOME -> DashboardRoute(onOpenCharge = onOpenCharge, onOpenRiding = onOpenRiding, onOpenMap = { tab = MainTab.MAP.ordinal })
                MainTab.RANKING -> RankingRoute()
                MainTab.MAP -> MapRoute()
                MainTab.ME -> MyCarRoute(onOpenSkin = onOpenSkin, group = MyCarGroup.ME)
                MainTab.SETTINGS -> MyCarRoute(onOpenSkin = onOpenSkin, group = MyCarGroup.SETTINGS)
            }
        }
    }
    Scaffold(containerColor = Pleos.colors.background) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            if (maxWidth.value >= TWO_COLUMN_MIN_WIDTH_DP) {
                Row(Modifier.fillMaxSize()) {
                    NavCard(items, tab, { tab = it }, vertical = true, trailingCount = 1, modifier = Modifier.padding(start = Space.l, top = Space.l, bottom = Space.l))
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    NavCard(items, tab, { tab = it }, vertical = false, trailingCount = 1, modifier = Modifier.padding(start = Space.l, end = Space.l, bottom = Space.l))
                }
            }
        }
    }
}

private const val TAB_FADE_MS = 260
/** The incoming page lifts in from this fraction of its height, a nudge rather than a slide. */
private const val TAB_LIFT_FRACTION = 24
