package com.example.pleos.ui.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.pleos.R
import com.example.pleos.domain.eco.EcoGuideState
import com.example.pleos.domain.eco.EcoLevel
import com.example.pleos.domain.eco.EcoReason
import com.example.pleos.domain.eco.EcoVerdict
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.domain.usecase.MonthSummary
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.AdaptiveScreen
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.PaneColumn
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosCard
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.Section
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.StatusLabel
import com.example.pleos.ui.design.TitleWithStatus
import com.example.pleos.ui.design.UNKNOWN
import com.example.pleos.ui.design.ValueBlock
import com.example.pleos.ui.design.WindowWidth
import com.example.pleos.ui.design.TwoColumns
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme

/**
 * Home tab, the screen used while driving (docs/visual/layout-variants.html, home B). In the
 * two-thirds window the left third holds what is always looked at (status, eco guide), next to the
 * driving view; the right holds what to answer now and this month's driving and spending. In the
 * one-third window the same sections stack.
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
        AdaptiveScreen(Modifier.padding(innerPadding)) { window ->
            when (window.width) {
                WindowWidth.TWO -> TwoColumns(
                    leftWeight = 1f,
                    rightWeight = 2f,
                    left = {
                        Section(stringResource(R.string.home_section_status), first = true) {
                            StatusCard(state, stacked = true)
                            EcoGuideCard(state.ecoGuide, onEvent)
                        }
                    },
                    right = {
                        Section(stringResource(R.string.home_section_action), first = true) { ActionCard(state.action, state.isAddingStop, onEvent) }
                        state.month?.let { MonthSection(it, onEvent) }
                    },
                )
                WindowWidth.ONE -> PaneColumn {
                    Section(stringResource(R.string.home_section_status), first = true) {
                        StatusCard(state, stacked = false)
                        EcoGuideCard(state.ecoGuide, onEvent)
                    }
                    Section(stringResource(R.string.home_section_action)) { ActionCard(state.action, state.isAddingStop, onEvent) }
                    state.month?.let { MonthSection(it, onEvent) }
                }
            }
        }
    }
}

/** Battery (or fuel), range and odometer at the value size. Stacked in the narrow left column. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(state: DashboardState, stacked: Boolean) {
    val energy = if (state.batteryPercent != null || state.fuelPercent == null) {
        stringResource(R.string.dashboard_battery) to (state.batteryPercent?.let { "$it%" } ?: UNKNOWN)
    } else {
        stringResource(R.string.dashboard_fuel) to "${state.fuelPercent}%"
    }
    val range = stringResource(R.string.dashboard_range) to (state.rangeKm?.let { Format.km(it.toDouble()) } ?: UNKNOWN)
    val odometer = stringResource(R.string.dashboard_odometer) to (state.odometerKm?.let(Format::km) ?: UNKNOWN)
    PleosCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        if (stacked) {
            listOf(energy, range, odometer).forEach { (label, value) -> ValueBlock(label, value) }
        } else {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
                verticalArrangement = Arrangement.spacedBy(Space.m),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(energy, range, odometer).forEach { (label, value) -> ValueBlock(label, value, Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * The one well that asks the driver for something ([DashboardState.action]): the only filled box
 * on the screen, with one ink button; an accident gets the danger fill instead.
 */
@Composable
private fun ActionCard(action: HomeAction?, isAddingStop: Boolean, onEvent: (DashboardEvent) -> Unit) {
    val c = Pleos.colors
    when (action) {
        null -> PleosCard(Modifier.fillMaxWidth(), color = c.surfaceHigh) {
            Text(stringResource(R.string.home_no_action), style = PleosText.body, color = c.textSecondary)
        }
        HomeAction.Airbag -> PleosCard(Modifier.fillMaxWidth(), color = c.negative) {
            Text(stringResource(R.string.dashboard_airbag_deployed), style = PleosText.cardTitle, color = c.onAccent)
        }
        is HomeAction.ChargeStop -> {
            val stop = action.stop
            PleosCard(Modifier.fillMaxWidth(), color = c.surfaceHigh) {
                TitleWithStatus(stringResource(R.string.stop_title), stringResource(R.string.stop_status), c.negative)
                Text(
                    stop.remainingKm?.let {
                        stringResource(R.string.stop_route, stop.destinationName ?: "", Format.km(it), Format.km(stop.rangeKm))
                    } ?: stringResource(R.string.stop_range, Format.km(stop.rangeKm)),
                    style = PleosText.body,
                    color = c.textSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Text(
                        stringResource(R.string.stop_candidate, listOfNotNull(stop.name, stop.brand).joinToString(" "), Format.distance(stop.distanceKm)),
                        style = PleosText.itemTitle,
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (stop.isPreferredBrand) StatusLabel(stringResource(R.string.stop_preferred), c.textSecondary)
                }
                if (stop.isDemoData) {
                    Text(stringResource(R.string.dashboard_stop_demo_data), style = PleosText.label, color = c.textTertiary)
                }
                // Confirm on the left, dismiss on the right (System Popup rule).
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    PleosButton(
                        text = stringResource(R.string.dashboard_stop_add),
                        onClick = { onEvent(DashboardEvent.AddChargeStopTapped) },
                        style = ButtonStyle.Filled,
                        enabled = !isAddingStop,
                    )
                    PleosButton(
                        text = stringResource(R.string.dashboard_stop_dismiss),
                        onClick = { onEvent(DashboardEvent.DismissChargeStopTapped) },
                        enabled = !isAddingStop,
                    )
                }
            }
        }
        is HomeAction.PendingCharge -> {
            val charge = action.charge
            PleosCard(Modifier.fillMaxWidth(), color = c.surfaceHigh, onClick = { onEvent(DashboardEvent.OpenChargeTapped) }) {
                TitleWithStatus(
                    stringResource(if (charge.isEv) R.string.pending_title_charge else R.string.pending_title_refuel),
                    stringResource(R.string.pending_status),
                    c.accent,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(charge.amount?.let { Format.energy(it, charge.isEv) }, charge.estimatedWon?.let(Format::won)).joinToString(", "),
                        style = PleosText.value,
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    PleosButton(
                        text = stringResource(R.string.dashboard_charge_open),
                        onClick = { onEvent(DashboardEvent.OpenChargeTapped) },
                        style = ButtonStyle.Filled,
                    )
                }
            }
        }
        is HomeAction.Warnings -> PleosCard(Modifier.fillMaxWidth(), color = c.surfaceHigh) {
            TitleWithStatus(stringResource(R.string.warnings_title), stringResource(R.string.warnings_status), c.negative)
            action.warnings.forEach { Text(it.label(), style = PleosText.body, color = c.textSecondary) }
        }
    }
}

/**
 * Eco driving guide: card title, one status word, the light it sends, one button. The status is
 * tinted like the mood light, darkened for 4.5:1 text contrast (UI_DESIGN_GUIDE).
 */
@Composable
private fun EcoGuideCard(guide: EcoGuideState, onEvent: (DashboardEvent) -> Unit) {
    val c = Pleos.colors
    PleosCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.eco_title), style = PleosText.cardTitle, color = c.textPrimary)
                if (guide.enabled) {
                    val (status, color, light) = when (guide.verdict.level) {
                        EcoLevel.GOOD -> Triple(stringResource(R.string.eco_good), c.textPrimary, R.string.eco_light_base)
                        EcoLevel.CAUTION -> Triple(stringResource(R.string.eco_caution, guide.verdict.reason.label()), CAUTION_TEXT, R.string.eco_light_caution)
                        EcoLevel.BAD -> Triple(stringResource(R.string.eco_bad, guide.verdict.reason.label()), c.negative, R.string.eco_light_bad)
                    }
                    StatusLabel(status, color)
                    Text(stringResource(light), style = PleosText.label, color = c.textTertiary)
                } else {
                    Text(stringResource(R.string.eco_off_hint), style = PleosText.body, color = c.textSecondary)
                }
            }
            PleosButton(
                text = stringResource(if (guide.enabled) R.string.eco_turn_off else R.string.eco_turn_on),
                onClick = { onEvent(DashboardEvent.EcoGuideToggleTapped) },
                style = if (guide.enabled) ButtonStyle.Basic else ButtonStyle.Filled,
            )
        }
    }
}

/** Dark orange for text; the mood light itself is [com.example.pleos.domain.eco.EcoDriveGuide.CAUTION_COLOR]. */
private val CAUTION_TEXT = Color(0xFFB85C00)

@Composable
private fun EcoReason?.label(): String = when (this) {
    EcoReason.HARD_ACCELERATION -> stringResource(R.string.eco_reason_accel)
    EcoReason.HARD_BRAKING -> stringResource(R.string.eco_reason_brake)
    EcoReason.HIGH_SPEED -> stringResource(R.string.eco_reason_speed)
    null -> ""
}

@Composable
private fun MonthSection(month: MonthSummary, onEvent: (DashboardEvent) -> Unit) {
    Section(
        stringResource(R.string.home_section_month),
        action = stringResource(R.string.home_open_ledger),
        onAction = { onEvent(DashboardEvent.OpenLedgerTapped) },
    ) {
        PleosCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.home_month_body, month.month, month.energyRecords, Format.won(month.spentWon)),
                style = PleosText.itemTitle,
                color = Pleos.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun VehicleWarning.label(): String = stringResource(
    when (this) {
        VehicleWarning.ENGINE_OIL -> R.string.warning_engine_oil
        VehicleWarning.BRAKE_FLUID -> R.string.warning_brake_fluid
        VehicleWarning.TRANSMISSION_OIL -> R.string.warning_transmission_oil
        VehicleWarning.LOW_TIRE_PRESSURE -> R.string.warning_low_tire_pressure
        VehicleWarning.WASHER_FLUID -> R.string.warning_washer_fluid
        VehicleWarning.POWER_DOWN -> R.string.warning_power_down
        VehicleWarning.AIRBAG_FAULT -> R.string.warning_airbag_fault
        VehicleWarning.SEAT_BELT -> R.string.warning_seat_belt
    },
)

private val previewState = DashboardState(
    batteryPercent = 23,
    rangeKm = 90,
    odometerKm = 12_370.0,
    chargeStop = ChargeStopUi("시연용 충전소 · 여의도", 5.5, 90.0, 160.0, "대전", isDemoData = true, brand = "한국전력", isPreferredBrand = true),
    ecoGuide = EcoGuideState(enabled = true, verdict = EcoVerdict(EcoLevel.CAUTION, EcoReason.HIGH_SPEED)),
    month = MonthSummary(month = 11, spentWon = 38_900, energyRecords = 3, energyAmount = 112.0),
)

@Preview(name = "Two thirds", showBackground = true, widthDp = 1700, heightDp = 1000)
@Composable
private fun DashboardTwoPreview() {
    PleosTheme { DashboardScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "Two thirds, low", showBackground = true, widthDp = 1274, heightDp = 700)
@Composable
private fun DashboardTwoLowPreview() {
    PleosTheme { DashboardScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "One third", showBackground = true, widthDp = 840, heightDp = 1000)
@Composable
private fun DashboardOnePreview() {
    PleosTheme { DashboardScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "One third, dark", showBackground = true, widthDp = 628, heightDp = 700, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardDarkPreview() {
    PleosTheme(darkTheme = true) { DashboardScreen(previewState, {}, SnackbarHostState()) }
}
