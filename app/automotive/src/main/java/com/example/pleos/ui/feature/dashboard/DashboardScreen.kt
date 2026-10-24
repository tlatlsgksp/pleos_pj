package com.example.pleos.ui.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.ui.theme.PleosTheme

/** Stateless dashboard. Everything it shows comes from [state]; everything it does goes out as an event. */
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (state.isDemo) {
                    Spacer(Modifier.width(12.dp))
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.dashboard_demo_badge)) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                ValueCard(
                    label = stringResource(R.string.dashboard_fuel),
                    value = state.fuelPercent?.let { "$it%" },
                    modifier = Modifier.weight(1f),
                )
                ValueCard(
                    label = stringResource(R.string.dashboard_range),
                    value = state.rangeKm?.let { "$it km" },
                    modifier = Modifier.weight(1f),
                )
                ValueCard(
                    label = stringResource(R.string.dashboard_driving_mode),
                    value = state.drivingMode?.let { it.label() },
                    modifier = Modifier.weight(1f),
                )
            }

            ValueCard(
                label = stringResource(R.string.dashboard_trip_since_refill),
                value = state.tripSinceRefillKm?.let { "%.1f km".format(it) },
                modifier = Modifier.fillMaxWidth(),
            )

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
private fun DrivingMode.label(): String = stringResource(
    when (this) {
        DrivingMode.ECO -> R.string.driving_mode_eco
        DrivingMode.NORMAL -> R.string.driving_mode_normal
        DrivingMode.SPORT -> R.string.driving_mode_sport
        DrivingMode.SNOW -> R.string.driving_mode_snow
        DrivingMode.CUSTOM -> R.string.driving_mode_custom
        DrivingMode.UNKNOWN -> R.string.driving_mode_unknown
    },
)

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

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun DashboardScreenPreview() {
    PleosTheme {
        DashboardScreen(
            state = DashboardState(
                fuelPercent = 91,
                rangeKm = 660,
                drivingMode = DrivingMode.ECO,
                tripSinceRefillKm = 24.0,
                warnings = setOf(VehicleWarning.ENGINE_OIL),
                isDemo = true,
            ),
            onEvent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
