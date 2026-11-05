package com.example.pleos.ui.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.feature.care.CareItemCard
import com.example.pleos.ui.feature.care.CareItemUi
import com.example.pleos.ui.theme.PleosTheme
import java.time.LocalDate

/** Stateless home. Everything it shows comes from [state]; everything it does goes out as an event. */
@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineMedium)
                if (state.isDemo) {
                    Spacer(Modifier.width(12.dp))
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.dashboard_demo_badge)) })
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onEvent(DashboardEvent.SourceToggleTapped) }) {
                    Text(
                        stringResource(if (state.isDemo) R.string.dashboard_use_real else R.string.dashboard_use_demo),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                if (state.batteryPercent != null || state.fuelPercent == null) {
                    ValueCard(stringResource(R.string.dashboard_battery), state.batteryPercent?.let { "$it%" }, Modifier.weight(1f))
                } else {
                    ValueCard(stringResource(R.string.dashboard_fuel), "${state.fuelPercent}%", Modifier.weight(1f))
                }
                ValueCard(stringResource(R.string.dashboard_range), state.rangeKm?.let { Format.km(it.toDouble()) }, Modifier.weight(1f))
                ValueCard(stringResource(R.string.dashboard_odometer), state.odometerKm?.let(Format::km), Modifier.weight(1f))
            }

            state.chargeStop?.let { stop ->
                ChargeStopCard(
                    stop = stop,
                    adding = state.isAddingStop,
                    onAdd = { onEvent(DashboardEvent.AddChargeStopTapped) },
                    onDismiss = { onEvent(DashboardEvent.DismissChargeStopTapped) },
                )
            }

            state.pendingCharge?.let { PendingChargeCard(it, onOpen = { onEvent(DashboardEvent.OpenChargeTapped) }) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.care_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(DashboardEvent.OpenCareTapped) }) {
                            Text(stringResource(R.string.dashboard_care_all), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    state.careItems.forEach { CareItemCard(item = it, saving = false, onServiced = null) }
                }
            }

            if (state.warnings.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_warnings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        state.warnings.forEach { Text(it.label()) }
                    }
                }
            }

            if (state.airbagDeployed) {
                Text(
                    text = stringResource(R.string.dashboard_airbag_deployed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.isDemo) {
                if (state.isDemoPlaying) {
                    OutlinedButton(onClick = { onEvent(DashboardEvent.StopDemoTapped) }) {
                        Text(stringResource(R.string.dashboard_demo_stop))
                    }
                } else {
                    Button(onClick = { onEvent(DashboardEvent.PlayDemoTapped) }) {
                        Text(stringResource(R.string.dashboard_demo_play))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChargeStopCard(stop: ChargeStopUi, adding: Boolean, onAdd: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val onError = MaterialTheme.colorScheme.onError
            Text(
                stringResource(R.string.dashboard_stop_title, Format.km(stop.rangeKm)),
                style = MaterialTheme.typography.titleSmall,
                color = onError,
            )
            stop.remainingKm?.let {
                Text(
                    stringResource(R.string.dashboard_stop_route, stop.destinationName ?: "", Format.km(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = onError,
                )
            }
            Text(
                stringResource(R.string.dashboard_stop_candidate, stop.name, "%.1f km".format(stop.distanceKm)),
                style = MaterialTheme.typography.bodyMedium,
                color = onError,
            )
            if (stop.isDemoData) {
                Text(stringResource(R.string.dashboard_stop_demo_data), style = MaterialTheme.typography.labelMedium, color = onError)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAdd, enabled = !adding) {
                    Text(stringResource(R.string.dashboard_stop_add), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onDismiss, enabled = !adding) {
                    Text(stringResource(R.string.dashboard_stop_dismiss), style = MaterialTheme.typography.labelLarge, color = onError)
                }
            }
        }
    }
}

@Composable
private fun PendingChargeCard(charge: PendingChargeUi, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (charge.isEv) R.string.dashboard_charge_detected else R.string.dashboard_refuel_detected),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
                val amount = charge.amount?.let { if (charge.isEv) Format.kwh(it) else "%.1f L".format(it) }
                val parts = listOfNotNull(amount, charge.estimatedWon?.let(Format::won))
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondary)
            }
            Button(onClick = onOpen) {
                Text(stringResource(R.string.dashboard_charge_open), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ValueCard(label: String, value: String?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value ?: stringResource(R.string.dashboard_value_unknown),
                style = MaterialTheme.typography.headlineSmall,
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

@Preview(showBackground = true, widthDp = 1024, heightDp = 1000)
@Composable
private fun DashboardScreenPreview() {
    PleosTheme {
        DashboardScreen(
            state = DashboardState(
                batteryPercent = 69,
                rangeKm = 267,
                odometerKm = 12_370.0,
                pendingCharge = PendingChargeUi("r1", 40.0, isEv = true, estimatedWon = 12_976),
                chargeStop = ChargeStopUi("시연용 충전소 · 강남역", 8.1, 90.0, 160.0, "대전", isDemoData = true),
                careItems = listOf(
                    CareItemUi("cabin_filter", "에어컨 필터", CareStatus.DUE_SOON, 0.85, 2_250.0, LocalDate.of(2027, 3, 1), 15_000, 12),
                    CareItemUi("wiper", "와이퍼", CareStatus.UNKNOWN, null, null, null, null, 12),
                ),
                isDemo = true,
            ),
            onEvent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
