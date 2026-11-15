package com.example.pleos.ui.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.pleos.R
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.usecase.MonthLedger
import com.example.pleos.domain.usecase.SpendGroup
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.AdaptiveScreen
import com.example.pleos.ui.design.AmountField
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.EmptyDetail
import com.example.pleos.ui.design.FactRow
import com.example.pleos.ui.design.FilterChips
import com.example.pleos.ui.design.InlineBar
import com.example.pleos.ui.design.LevelMeter
import com.example.pleos.ui.design.LineTabs
import com.example.pleos.ui.design.PaneColumn
import com.example.pleos.ui.design.PleosBackLink
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.ROW_RADIUS
import com.example.pleos.ui.design.RULE
import com.example.pleos.ui.design.Section
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.SpineGap
import com.example.pleos.ui.design.SpineRow
import com.example.pleos.ui.design.TwoColumns
import com.example.pleos.ui.design.UNKNOWN
import com.example.pleos.ui.design.WindowInfo
import com.example.pleos.ui.design.WindowWidth
import com.example.pleos.ui.design.bleed
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme
import java.time.LocalDate

/**
 * Fuel and charge tab (docs/visual/ledger-design-v5.html): statistics and records under line
 * tabs. In the two-thirds window each is list | detail, so picking changes only the right column;
 * in the one-third window the detail replaces the list, with a back link.
 */
@Composable
fun LedgerScreen(
    state: LedgerState,
    onEvent: (LedgerEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Pleos.colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        AdaptiveScreen(Modifier.padding(innerPadding)) { window ->
            Column(Modifier.fillMaxSize()) {
                val tabs = listOf(stringResource(R.string.ledger_tab_stats), stringResource(R.string.ledger_tab_records))
                LineTabs(tabs, state.tab.ordinal, { onEvent(LedgerEvent.TabSelected(LedgerTab.entries[it])) })
                HorizontalDivider(color = Pleos.colors.outline, thickness = RULE)
                Spacer(Modifier.padding(top = Space.m))
                Box(Modifier.weight(1f)) {
                    when (state.tab) {
                        LedgerTab.STATS -> StatsTab(state, onEvent, window)
                        LedgerTab.RECORDS -> RecordsTab(state, onEvent, window)
                    }
                }
            }
        }
    }
}

/** List | detail in the two-thirds window; the detail in place of the list in the one-third window. */
@Composable
private fun ListDetail(
    window: WindowInfo,
    hasSelection: Boolean,
    onBack: () -> Unit,
    list: @Composable ColumnScope.(twoColumns: Boolean) -> Unit,
    detail: @Composable ColumnScope.() -> Unit,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
) {
    when (window.width) {
        WindowWidth.TWO -> TwoColumns(leftWeight, rightWeight, left = { list(true) }, right = detail)
        WindowWidth.ONE -> PaneColumn {
            if (hasSelection) {
                PleosBackLink(stringResource(R.string.ledger_back), onBack)
                detail()
            } else {
                list(false)
            }
        }
    }
}

// Statistics -----------------------------------------------------------------------------------

@Composable
private fun StatsTab(state: LedgerState, onEvent: (LedgerEvent) -> Unit, window: WindowInfo) {
    val month = state.months.getOrNull(state.monthIndex) ?: return
    val summary: @Composable ColumnScope.() -> Unit = { MonthSummaryBlock(state, month, onEvent) }
    val table: @Composable ColumnScope.(Boolean) -> Unit = { wide -> MonthTable(state, onEvent, wide) }
    when (window.width) {
        WindowWidth.TWO -> TwoColumns(1f, 1.25f, left = summary, right = { table(true) })
        WindowWidth.ONE -> PaneColumn {
            summary()
            Spacer(Modifier.padding(top = Space.xl))
            table(false)
        }
    }
}

@Composable
private fun MonthSummaryBlock(state: LedgerState, month: MonthLedger, onEvent: (LedgerEvent) -> Unit) {
    val c = Pleos.colors
    val older = state.months.getOrNull(state.monthIndex + 1)
    val newer = state.months.getOrNull(state.monthIndex - 1)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        StepLink(older?.let { stringResource(R.string.stats_prev, it.month) }) { onEvent(LedgerEvent.MonthStepped(-1)) }
        Text(stringResource(R.string.stats_month, month.year, month.month), style = PleosText.itemTitle, color = c.textPrimary)
        StepLink(newer?.let { stringResource(R.string.stats_next, it.month) }) { onEvent(LedgerEvent.MonthStepped(1)) }
    }
    Spacer(Modifier.padding(top = Space.m))
    if (month.spentWon == 0L && month.groups.isEmpty()) {
        Text(stringResource(R.string.stats_empty), style = PleosText.body, color = c.textSecondary)
        return
    }
    Text(
        month.distanceKm?.let { stringResource(R.string.stats_sentence, Format.km(it), Format.won(month.spentWon)) }
            ?: stringResource(R.string.stats_sentence_spent, Format.won(month.spentWon)),
        style = PleosText.sentence,
        color = c.textPrimary,
    )
    val notes = buildList {
        older?.let {
            val diff = month.spentWon - it.spentWon
            add(
                when {
                    diff < 0 -> stringResource(R.string.stats_compare_less, Format.won(-diff))
                    diff > 0 -> stringResource(R.string.stats_compare_more, Format.won(diff))
                    else -> stringResource(R.string.stats_compare_same)
                },
            )
        }
        month.distanceKm?.takeIf { it >= 1 }?.let { add(stringResource(R.string.stats_per_km, Format.won(Math.round(month.spentWon / it)))) }
    }
    if (notes.isNotEmpty()) {
        Text(notes.joinToString(" "), style = PleosText.body, color = c.textSecondary, modifier = Modifier.padding(top = Space.s))
    }
    Spacer(Modifier.padding(top = Space.m))
    month.groups.forEach { group ->
        HorizontalDivider(color = c.outline, thickness = RULE)
        Row(Modifier.fillMaxWidth().padding(vertical = Space.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(stringResource(R.string.stats_group, group.label(state.isEv), group.count), style = PleosText.itemTitle, color = c.textPrimary, modifier = Modifier.weight(1f))
            Text(Format.won(group.won), style = PleosText.figure, color = c.textPrimary)
            Text(
                Format.percent(if (month.spentWon > 0) group.won * 100.0 / month.spentWon else 0.0),
                style = PleosText.labelFigure,
                color = c.textTertiary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.25f),
            )
        }
    }
}

@Composable
private fun StepLink(text: String?, onClick: () -> Unit) {
    if (text == null) return
    val interaction = remember { MutableInteractionSource() }
    Text(
        text,
        style = PleosText.label,
        color = Pleos.colors.textTertiary,
        modifier = Modifier.heightIn(min = Space.xl + Space.s).clickable(interaction, indication = null, onClick = onClick).padding(vertical = Space.s),
    )
}

/** Monthly ledger: one row a month, the spending bar inside its cell. [wide] adds the energy columns. */
@Composable
private fun MonthTable(state: LedgerState, onEvent: (LedgerEvent) -> Unit, wide: Boolean) {
    val c = Pleos.colors
    val maxWon = state.months.maxOfOrNull { it.spentWon }?.coerceAtLeast(1L) ?: 1L
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.stats_table), style = PleosText.sectionTitle, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (wide) Text(stringResource(if (state.isEv) R.string.stats_unit_ev else R.string.stats_unit_fuel), style = PleosText.label, color = c.textTertiary)
    }
    Spacer(Modifier.padding(top = Space.s))
    TableRow(header = true) {
        Cell(stringResource(R.string.stats_col_month), 0.7f, header = true, start = true)
        Cell(stringResource(R.string.stats_col_distance), 1f, header = true)
        if (wide) {
            Cell(stringResource(if (state.isEv) R.string.stats_energy_ev else R.string.stats_energy_fuel), 1.1f, header = true)
            Cell(stringResource(if (state.isEv) R.string.stats_col_eff_ev else R.string.stats_col_eff_fuel), 0.7f, header = true)
        }
        Cell(stringResource(R.string.stats_col_spent), 1.9f, header = true)
    }
    state.months.forEachIndexed { index, m ->
        val selected = index == state.monthIndex
        TableRow(selected = selected, onClick = { onEvent(LedgerEvent.MonthStepped(state.monthIndex - index)) }) {
            Cell(stringResource(R.string.ledger_month, m.month), 0.7f, strong = selected, start = true)
            Cell(m.distanceKm?.let(Format::km) ?: UNKNOWN, 1f, strong = selected)
            if (wide) {
                Cell(if (m.energyAmount > 0) Format.energy(m.energyAmount, state.isEv) else UNKNOWN, 1.1f, strong = selected)
                val efficiency = m.distanceKm?.takeIf { m.energyAmount > 0 }?.let { "%.1f".format(it / m.energyAmount) }
                Cell(efficiency ?: UNKNOWN, 0.7f, strong = selected)
            }
            Row(Modifier.weight(1.9f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.End)) {
                InlineBar(m.spentWon.toFloat() / maxWon, highlighted = index == 0)
                Text(Format.won(m.spentWon), style = PleosText.figure, color = if (selected) c.textPrimary else c.textSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TableRow(header: Boolean = false, selected: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().bleed(Space.s)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (header) Space.xl else Space.xl + Space.m)
                .clip(RoundedCornerShape(ROW_RADIUS))
                .background(if (selected) c.surfaceHigh else Color.Transparent)
                .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
                .padding(horizontal = Space.s),
            content = content,
        )
        HorizontalDivider(color = c.outline, thickness = RULE, modifier = Modifier.padding(horizontal = Space.s))
    }
}

@Composable
private fun RowScope.Cell(text: String, weight: Float, header: Boolean = false, strong: Boolean = false, start: Boolean = false) {
    val c = Pleos.colors
    val style: TextStyle = if (header) PleosText.label else PleosText.figure
    Text(
        text,
        style = style,
        color = when {
            header -> c.textTertiary
            strong -> c.textPrimary
            else -> c.textSecondary
        },
        textAlign = if (start) TextAlign.Start else TextAlign.End,
        maxLines = 1,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun SpendGroup.label(isEv: Boolean): String = when (type) {
    RecordType.ENERGY -> stringResource(if (isEv) R.string.stats_energy_ev else R.string.stats_energy_fuel)
    RecordType.MAINTENANCE -> stringResource(R.string.stats_maintenance)
    RecordType.ACCIDENT -> stringResource(R.string.stats_accident)
    RecordType.EXPENSE -> category?.label() ?: ""
}

// Records --------------------------------------------------------------------------------------

@Composable
private fun RecordsTab(state: LedgerState, onEvent: (LedgerEvent) -> Unit, window: WindowInfo) {
    ListDetail(
        window = window,
        hasSelection = state.selectedRecord != null,
        onBack = { onEvent(LedgerEvent.BackTapped) },
        list = { twoColumns -> Spine(state, onEvent, withOdometer = twoColumns) },
        detail = {
            state.selectedRecord?.let { RecordDetail(it, state, onEvent) } ?: EmptyDetail(stringResource(R.string.ledger_pick_record))
        },
    )
}

/** Records on the odometer axis, grouped by month, with the distance driven between them. */
@Composable
private fun Spine(state: LedgerState, onEvent: (LedgerEvent) -> Unit, withOdometer: Boolean) {
    val c = Pleos.colors
    val filters = listOf(
        stringResource(R.string.ledger_filter_all),
        stringResource(R.string.ledger_filter_energy),
        stringResource(R.string.ledger_filter_expense),
    )
    FilterChips(filters, state.filter.ordinal, { onEvent(LedgerEvent.FilterSelected(RecordFilter.entries[it])) })
    if (!state.isLoading && state.items.isEmpty()) {
        Text(stringResource(R.string.ledger_empty), style = PleosText.body, color = c.textSecondary, modifier = Modifier.padding(top = Space.m))
        return
    }
    state.items.groupBy { it.date.year to it.date.monthValue }.forEach { (key, items) ->
        Text(
            stringResource(R.string.ledger_month, key.second),
            style = PleosText.label,
            color = c.textTertiary,
            modifier = Modifier.padding(top = Space.m, bottom = Space.xs),
        )
        items.forEach { item ->
            SpineRow(
                odometer = if (withOdometer) item.odometerKm?.let(Format::km) ?: UNKNOWN else null,
                title = item.title,
                time = item.detail,
                amount = item.amountWon?.let(Format::won) ?: UNKNOWN,
                flag = if (item.isDraft) stringResource(R.string.ledger_draft) else null,
                selected = (state.selection as? LedgerSelection.Record)?.id == item.id,
                onClick = { onEvent(LedgerEvent.RecordTapped(item.id)) },
            )
            item.gapKm?.let { SpineGap(stringResource(R.string.ledger_gap, Format.km(it)), withOdometer) }
        }
    }
}

/** The selected record read like a receipt; a draft's amount is a field to check and save. */
@Composable
private fun RecordDetail(item: LedgerItemUi, state: LedgerState, onEvent: (LedgerEvent) -> Unit) {
    val c = Pleos.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Text(stringResource(R.string.ledger_detail_title, item.detail.orEmpty(), item.title), style = PleosText.cardTitle, color = c.textPrimary)
        if (item.isDraft) {
            Text(stringResource(R.string.ledger_draft_hint), style = PleosText.body, color = c.textSecondary)
            Spacer(Modifier.padding(top = Space.xs))
            AmountField(state.amountInput, { onEvent(LedgerEvent.AmountChanged(it)) }, stringResource(R.string.ledger_won))
            item.unitPriceText?.let { Text(stringResource(R.string.ledger_unit_price, it), style = PleosText.label, color = c.textTertiary) }
        } else {
            Text(item.amountWon?.let(Format::won) ?: UNKNOWN, style = PleosText.value, color = c.textPrimary)
        }
        if (item.levelBefore != null && item.levelAfter != null) {
            Spacer(Modifier.padding(top = Space.xs))
            LevelMeter(
                before = item.levelBefore,
                after = item.levelAfter,
                startLabel = stringResource(R.string.ledger_level_before, Format.percent(item.levelBefore)),
                middleLabel = item.energyText.orEmpty(),
                endLabel = stringResource(R.string.ledger_level_after, Format.percent(item.levelAfter)),
            )
        }
        Column(Modifier.padding(top = Space.s)) { item.facts.forEach { (label, value) -> FactRow(label, value) } }
        if (item.isDraft) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.padding(top = Space.s)) {
                PleosButton(
                    text = stringResource(R.string.ledger_confirm),
                    onClick = { onEvent(LedgerEvent.SaveDraftTapped(item.id)) },
                    style = ButtonStyle.Filled,
                    enabled = !state.isSaving,
                )
                PleosButton(
                    text = stringResource(R.string.ledger_dismiss),
                    onClick = { onEvent(LedgerEvent.DismissDraftTapped(item.id)) },
                    enabled = !state.isSaving,
                )
            }
        }
    }
}

// Previews -------------------------------------------------------------------------------------

private val previewItems = listOf(
    LedgerItemUi(
        "d1", LocalDate.of(2026, 11, 14), RecordType.ENERGY, "충전 80.0 kWh", "11월 14일 11:43", 25_952, isDraft = true,
        facts = listOf("충전량" to "80.0 kWh", "주행거리" to "12,370 km", "기록 방식" to "차량이 감지"),
        odometerKm = 12_370.0, levelBefore = 17.0, levelAfter = 69.0, energyText = "80.0 kWh", unitPriceText = "kWh당 324.4원", gapKm = 25.0,
    ),
    LedgerItemUi("r1", LocalDate.of(2026, 11, 14), RecordType.ENERGY, "충전 4.5 kWh", "11월 14일 10:58", 1_460, isDraft = false, odometerKm = 12_345.0, gapKm = 235.0),
    LedgerItemUi("r2", LocalDate.of(2026, 11, 2), RecordType.EXPENSE, "세차", "11월 2일 11:40", 3_900, isDraft = false, odometerKm = 12_110.0, gapKm = 208.0),
    LedgerItemUi("r3", LocalDate.of(2026, 10, 28), RecordType.MAINTENANCE, "에어컨 필터 교체", "10월 28일 14:10", 25_000, isDraft = false, odometerKm = 11_902.0),
)

private val previewMonths = listOf(
    MonthLedger(2026, 11, 412.0, 112.0, 38_900, listOf(SpendGroup(RecordType.ENERGY, null, 3, 24_100), SpendGroup(RecordType.MAINTENANCE, null, 1, 10_900))),
    MonthLedger(2026, 10, 389.0, 78.2, 44_200, emptyList()),
    MonthLedger(2026, 9, 455.0, 90.1, 51_600, emptyList()),
    MonthLedger(2026, 8, 301.0, 61.0, 33_800, emptyList()),
)

private val previewState = LedgerState(
    items = previewItems,
    months = previewMonths,
    selection = LedgerSelection.Record("d1"),
    amountInput = "25952",
    isLoading = false,
)

@Preview(name = "Records, two thirds", showBackground = true, widthDp = 1700, heightDp = 1000)
@Composable
private fun LedgerRecordsPreview() {
    PleosTheme { LedgerScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "Stats, two thirds", showBackground = true, widthDp = 1700, heightDp = 1000)
@Composable
private fun LedgerStatsPreview() {
    PleosTheme { LedgerScreen(previewState.copy(tab = LedgerTab.STATS), {}, SnackbarHostState()) }
}

@Preview(name = "Records, one third", showBackground = true, widthDp = 628, heightDp = 700)
@Composable
private fun LedgerOneListPreview() {
    PleosTheme { LedgerScreen(previewState.copy(selection = null), {}, SnackbarHostState()) }
}
