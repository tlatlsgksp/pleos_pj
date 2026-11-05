package com.example.pleos.ui.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.ButtonSize
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.CardGap
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosCard
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.ScreenPadding
import com.example.pleos.ui.design.StatusLabel
import com.example.pleos.ui.design.TWO_PANE_MIN_WIDTH_DP
import com.example.pleos.ui.design.ValueBlock
import com.example.pleos.ui.feature.care.CareItemCard
import com.example.pleos.ui.feature.care.CareItemUi
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme
import java.time.LocalDate

/**
 * Stateless home. In a landscape window the vehicle status and the cards that need an answer sit
 * on the left and the care preview on the right; in portrait they stack (UI_DESIGN_GUIDE 2).
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
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            val twoPane = maxWidth.value >= TWO_PANE_MIN_WIDTH_DP
            val valuesPerRow = if (maxWidth < 760.dp) 2 else 3
            Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
                PageNavigation(title = stringResource(R.string.dashboard_title)) {
                    if (state.isDemo) {
                        StatusLabel(stringResource(R.string.dashboard_demo_badge), Pleos.colors.positive)
                        Spacer(Modifier.width(16.dp))
                    }
                    PleosButton(
                        text = stringResource(if (state.isDemo) R.string.dashboard_use_real else R.string.dashboard_use_demo),
                        onClick = { onEvent(DashboardEvent.SourceToggleTapped) },
                        size = ButtonSize.Small,
                    )
                }
                if (twoPane) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CardGap), modifier = Modifier.fillMaxSize()) {
                        Pane(Modifier.weight(1f)) {
                            StatusCard(state, valuesPerRow = 3)
                            ActionCards(state, onEvent)
                        }
                        Pane(Modifier.weight(1f)) { CarePreview(state, onEvent) }
                    }
                } else {
                    Pane(Modifier.fillMaxSize()) {
                        StatusCard(state, valuesPerRow = valuesPerRow)
                        ActionCards(state, onEvent)
                        CarePreview(state, onEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun Pane(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CardGap),
        modifier = modifier.verticalScroll(rememberScrollState()).padding(bottom = ScreenPadding),
        content = content,
    )
}

/** Battery (or fuel), range and odometer: the values a driver checks at a glance. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(state: DashboardState, valuesPerRow: Int) {
    val unknown = stringResource(R.string.dashboard_value_unknown)
    PleosCard(Modifier.fillMaxWidth()) {
        FlowRow(
            maxItemsInEachRow = valuesPerRow,
            horizontalArrangement = Arrangement.spacedBy(CardGap),
            verticalArrangement = Arrangement.spacedBy(CardGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val energy = if (state.batteryPercent != null || state.fuelPercent == null) {
                stringResource(R.string.dashboard_battery) to (state.batteryPercent?.let { "$it%" } ?: unknown)
            } else {
                stringResource(R.string.dashboard_fuel) to "${state.fuelPercent}%"
            }
            ValueBlock(energy.first, energy.second, Modifier.weight(1f))
            ValueBlock(stringResource(R.string.dashboard_range), state.rangeKm?.let { Format.km(it.toDouble()) } ?: unknown, Modifier.weight(1f))
            ValueBlock(stringResource(R.string.dashboard_odometer), state.odometerKm?.let(Format::km) ?: unknown, Modifier.weight(1f))
        }
    }
}

/** Cards that ask the driver for something, most urgent first. */
@Composable
private fun ActionCards(state: DashboardState, onEvent: (DashboardEvent) -> Unit) {
    val c = Pleos.colors
    state.chargeStop?.let { stop ->
        PleosCard(Modifier.fillMaxWidth(), color = c.negative) {
            Text(
                stringResource(R.string.dashboard_stop_title, Format.km(stop.rangeKm)),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = c.onAccent,
            )
            stop.remainingKm?.let {
                Text(
                    stringResource(R.string.dashboard_stop_route, stop.destinationName ?: "", Format.km(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onAccent,
                )
            }
            Text(
                stringResource(R.string.dashboard_stop_candidate, stop.name, "%.1f km".format(stop.distanceKm)),
                style = MaterialTheme.typography.bodyMedium,
                color = c.onAccent,
            )
            if (stop.isDemoData) {
                Text(stringResource(R.string.dashboard_stop_demo_data), style = MaterialTheme.typography.labelMedium, color = c.onAccent)
            }
            // Confirm on the left, dismiss on the right (System Popup rule).
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PleosButton(
                    text = stringResource(R.string.dashboard_stop_add),
                    onClick = { onEvent(DashboardEvent.AddChargeStopTapped) },
                    style = ButtonStyle.Filled,
                    enabled = !state.isAddingStop,
                )
                PleosButton(
                    text = stringResource(R.string.dashboard_stop_dismiss),
                    onClick = { onEvent(DashboardEvent.DismissChargeStopTapped) },
                    enabled = !state.isAddingStop,
                )
            }
        }
    }

    state.pendingCharge?.let { charge ->
        PleosCard(Modifier.fillMaxWidth(), onClick = { onEvent(DashboardEvent.OpenChargeTapped) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusLabel(
                        stringResource(if (charge.isEv) R.string.dashboard_charge_detected else R.string.dashboard_refuel_detected),
                        c.positive,
                    )
                    val amount = charge.amount?.let { if (charge.isEv) Format.kwh(it) else "%.1f L".format(it) }
                    Text(
                        listOfNotNull(amount, charge.estimatedWon?.let(Format::won)).joinToString(" · "),
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                }
                PleosButton(
                    text = stringResource(R.string.dashboard_charge_open),
                    onClick = { onEvent(DashboardEvent.OpenChargeTapped) },
                    style = ButtonStyle.Filled,
                )
            }
        }
    }

    if (state.warnings.isNotEmpty()) {
        PleosCard(Modifier.fillMaxWidth()) {
            StatusLabel(stringResource(R.string.dashboard_warnings), c.negative)
            state.warnings.forEach { Text(it.label(), style = MaterialTheme.typography.bodyMedium, color = c.textPrimary) }
        }
    }

    if (state.airbagDeployed) {
        PleosCard(Modifier.fillMaxWidth(), color = c.negative) {
            Text(stringResource(R.string.dashboard_airbag_deployed), style = MaterialTheme.typography.bodyLarge, color = c.onAccent)
        }
    }

    if (state.isDemo) {
        PleosButton(
            text = stringResource(if (state.isDemoPlaying) R.string.dashboard_demo_stop else R.string.dashboard_demo_play),
            onClick = { onEvent(if (state.isDemoPlaying) DashboardEvent.StopDemoTapped else DashboardEvent.PlayDemoTapped) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CarePreview(state: DashboardState, onEvent: (DashboardEvent) -> Unit) {
    val c = Pleos.colors
    PleosCard(Modifier.fillMaxWidth(), color = c.surfaceHigh, verticalArrangement = Arrangement.spacedBy(CardGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.care_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            PleosButton(
                text = stringResource(R.string.dashboard_care_all),
                onClick = { onEvent(DashboardEvent.OpenCareTapped) },
                size = ButtonSize.Small,
            )
        }
        state.careItems.forEach { CareItemCard(item = it, saving = false, onServiced = null) }
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
    pendingCharge = PendingChargeUi("r1", 40.0, isEv = true, estimatedWon = 12_976),
    chargeStop = ChargeStopUi("시연용 충전소 · 강남역", 8.1, 90.0, 160.0, "대전", isDemoData = true),
    careItems = listOf(
        CareItemUi("cabin_filter", "에어컨 필터", CareStatus.DUE_SOON, 0.85, 2_250.0, LocalDate.of(2027, 3, 1), 15_000, 12),
        CareItemUi("brake_fluid", "브레이크 오일", CareStatus.OK, 0.3, 21_000.0, LocalDate.of(2028, 1, 1), 30_000, 24),
        CareItemUi("wiper", "와이퍼", CareStatus.UNKNOWN, null, null, null, null, 12),
    ),
    isDemo = true,
)

@Preview(name = "Landscape window", showBackground = true, widthDp = 1700, heightDp = 1184)
@Composable
private fun DashboardLandscapePreview() {
    PleosTheme { DashboardScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "Portrait window", showBackground = true, widthDp = 840, heightDp = 1184)
@Composable
private fun DashboardPortraitPreview() {
    PleosTheme { DashboardScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "Dark, narrowest portrait", showBackground = true, widthDp = 628, heightDp = 855, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardDarkPreview() {
    PleosTheme(darkTheme = true) { DashboardScreen(previewState, {}, SnackbarHostState()) }
}
