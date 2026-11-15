package com.example.pleos.ui.feature.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.pleos.R
import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RankPeriod
import com.example.pleos.domain.model.RankScope
import com.example.pleos.domain.model.Ranking
import com.example.pleos.domain.repository.CommunityRepository
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.AdaptiveScreen
import com.example.pleos.ui.design.BoxTabs
import com.example.pleos.ui.design.HomeCard
import com.example.pleos.ui.design.InlineBar
import com.example.pleos.ui.design.MemberAvatar
import com.example.pleos.ui.design.NumberUnit
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.ROW_RADIUS
import com.example.pleos.ui.design.RankBadge
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.TrendChip
import com.example.pleos.ui.design.WindowWidth
import com.example.pleos.ui.design.mineMark
import com.example.pleos.ui.theme.Pleos
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** The ranking for the chosen scope and period, with me. */
data class RankingUi(val ranking: Ranking, val me: Member, val scope: RankScope, val period: RankPeriod)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RankingViewModel @Inject constructor(private val community: CommunityRepository) : ViewModel() {
    private val filter = MutableStateFlow(RankScope.MODEL to RankPeriod.WEEK)

    val state: StateFlow<RankingUi?> = filter
        .flatMapLatest { (scope, period) -> combine(community.ranking(scope, period), community.me) { r, me -> RankingUi(r, me, scope, period) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(scope: RankScope = filter.value.first, period: RankPeriod = filter.value.second) {
        filter.value = scope to period
    }
}

/** Efficiency ranking, same model by default: where I stand and why on the left, the leaders on the right. */
@Composable
fun RankingRoute(viewModel: RankingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { RankingScreen(it, onScope = { s -> viewModel.select(scope = s) }, onPeriod = { p -> viewModel.select(period = p) }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankingScreen(ui: RankingUi, onScope: (RankScope) -> Unit, onPeriod: (RankPeriod) -> Unit) {
    AdaptiveScreen(Modifier.padding(vertical = Space.l)) { window ->
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            // Left aligned: the window's left edge is nearest the driver.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                BoxTabs(
                    listOf(stringResource(R.string.rank_tab_model), stringResource(R.string.rank_tab_region), stringResource(R.string.rank_tab_neighbors)),
                    ui.scope.ordinal, { onScope(RankScope.entries[it]) },
                )
                BoxTabs(listOf(stringResource(R.string.rank_tab_week), stringResource(R.string.rank_tab_month)), ui.period.ordinal, { onPeriod(RankPeriod.entries[it]) })
            }
            if (window.width == WindowWidth.TWO) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    // My card is as tall as its content; the ground shows below it.
                    MyStanding(ui, Modifier.weight(1f))
                    Leaders(ui, Modifier.weight(1.4f).fillMaxHeight())
                }
            } else {
                MyStanding(ui, Modifier.fillMaxWidth())
                Leaders(ui, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

/** My rank as the headline, the change since the last period, then the three numbers that moved it. */
@Composable
private fun MyStanding(ui: RankingUi, modifier: Modifier) {
    val c = Pleos.colors
    val r = ui.ranking
    val week = ui.period == RankPeriod.WEEK
    HomeCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(
                stringResource(R.string.rank_scope, r.scopeName, stringResource(if (week) R.string.rank_tab_week else R.string.rank_tab_month)),
                style = PleosText.itemTitle, color = c.textSecondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                r.myRank?.let { NumberUnit("$it", stringResource(R.string.rank_unit), PleosText.value, PleosText.sectionTitle) }
                val myRank = r.myRank
                val previous = r.previousRank
                if (myRank != null && previous != null) TrendChip(previous - myRank, large = true)
            }
            r.myKmPerUnit?.let { NumberUnit(Format.efficiencyNumber(it), Format.efficiencyUnit(ui.me.isEv), PleosText.cardTitle, PleosText.label) }
        }
        Column(Modifier.padding(top = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text(stringResource(if (week) R.string.rank_week_driving else R.string.rank_month_driving), style = PleosText.sectionTitle, color = c.textPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Stat(stringResource(R.string.rank_hard_accel), "${r.hardAccelerations}", stringResource(R.string.rank_times_unit), Modifier.weight(1f))
                Stat(stringResource(R.string.rank_hard_brake), "${r.hardBrakings}", stringResource(R.string.rank_times_unit), Modifier.weight(1f))
                r.ecoModeShare?.let { Stat(stringResource(R.string.rank_eco_mode), "${(it * 100).roundToInt()}", "%", Modifier.weight(1f)) }
            }
        }
    }
}

/** One figure of the period's driving: the number first, its label under it. */
@Composable
private fun Stat(label: String, number: String, unit: String, modifier: Modifier) {
    val c = Pleos.colors
    Column(
        modifier.clip(RoundedCornerShape(ROW_RADIUS)).background(c.surfaceHigh).padding(Space.m),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        NumberUnit(number, unit, PleosText.cardTitle, PleosText.label)
        Text(label, style = PleosText.body, color = c.textSecondary, maxLines = 1)
    }
}

/** Leaders in order; my row marked, and pinned under the list when I am not among them. */
@Composable
private fun Leaders(ui: RankingUi, modifier: Modifier) {
    val c = Pleos.colors
    val r = ui.ranking
    val top = r.leaders.maxOfOrNull { it.kmPerUnit } ?: 1.0
    HomeCard(modifier) {
        Text(stringResource(R.string.rank_leaders), style = PleosText.sectionTitle, color = c.textPrimary)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            r.leaders.forEach { e -> LeaderRow(e.rank, e.member, e.kmPerUnit, top, mine = e.member.id == ui.me.id, change = e.change) }
        }
        val myRank = r.myRank
        val myValue = r.myKmPerUnit
        if (myRank != null && myValue != null && r.leaders.none { it.member.id == ui.me.id }) {
            Box(Modifier.padding(top = Space.xs)) { LeaderRow(myRank, ui.me, myValue, top, mine = true, change = r.previousRank?.let { it - myRank }) }
        }
    }
}

@Composable
private fun LeaderRow(rank: Int, member: Member, value: Double, top: Double, mine: Boolean, change: Int?) {
    val c = Pleos.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ROW_RADIUS))
            .background(if (mine) c.signBlue.copy(alpha = 0.08f) else c.surface)
            .mineMark(mine)
            .padding(horizontal = Space.s, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        RankBadge(rank)
        MemberAvatar(member.nickname, member.handle)
        Column(Modifier.weight(1f)) {
            Text(if (mine) stringResource(R.string.rank_me, member.nickname) else member.nickname, style = PleosText.itemTitle, color = c.textPrimary, maxLines = 1)
            Text(member.carModel, style = PleosText.label, color = c.textSecondary, maxLines = 1)
        }
        change?.let { TrendChip(it) }
        InlineBar((value / top).toFloat(), highlighted = true, color = if (mine) c.signBlue else c.signGreen)
        NumberUnit(Format.efficiencyNumber(value), Format.efficiencyUnit(member.isEv), PleosText.figure, PleosText.label)
    }
}
