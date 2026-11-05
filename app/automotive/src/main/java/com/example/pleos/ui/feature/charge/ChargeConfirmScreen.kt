package com.example.pleos.ui.feature.charge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.theme.PleosTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

@Composable
fun ChargeConfirmScreen(state: ChargeConfirmState, onEvent: (ChargeConfirmEvent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEvent(ChargeConfirmEvent.BackTapped) }) {
                    Text(stringResource(R.string.common_back), style = MaterialTheme.typography.labelLarge)
                }
                Text(stringResource(R.string.charge_title), style = MaterialTheme.typography.headlineMedium)
            }

            when {
                state.isLoading -> Unit
                state.isMissing -> Text(stringResource(R.string.charge_missing), style = MaterialTheme.typography.bodyLarge)
                else -> ChargeDetails(state, onEvent)
            }
        }
    }
}

@Composable
private fun ChargeDetails(state: ChargeConfirmState, onEvent: (ChargeConfirmEvent) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = state.amountKwh?.let(Format::kwh) ?: stringResource(R.string.dashboard_value_unknown),
                style = MaterialTheme.typography.displaySmall,
            )
            if (state.levelBefore != null && state.levelAfter != null) {
                Text(
                    stringResource(R.string.charge_level, Format.percent(state.levelBefore), Format.percent(state.levelAfter)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            val meta = listOfNotNull(state.occurredAt?.format(timeFormat), state.odometerKm?.let(Format::km))
            Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.charge_vehicle_checked), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }

    OutlinedTextField(
        value = state.amountInput,
        onValueChange = { onEvent(ChargeConfirmEvent.AmountChanged(it)) },
        label = { Text(stringResource(R.string.charge_amount_label)) },
        suffix = { Text("원") },
        supportingText = state.unitPriceWon?.let { { Text(stringResource(R.string.charge_estimate_hint, "%.1f".format(it))) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { onEvent(ChargeConfirmEvent.ConfirmTapped) }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.charge_confirm), style = MaterialTheme.typography.titleSmall)
        }
        OutlinedButton(onClick = { onEvent(ChargeConfirmEvent.DismissTapped) }, enabled = !state.isSaving) {
            Text(stringResource(R.string.charge_dismiss), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 800)
@Composable
private fun ChargeConfirmScreenPreview() {
    PleosTheme {
        ChargeConfirmScreen(
            state = ChargeConfirmState(
                isLoading = false,
                amountKwh = 40.0,
                levelBefore = 17.4,
                levelAfter = 69.1,
                odometerKm = 12_370.0,
                occurredAt = LocalDateTime.of(2026, 11, 5, 18, 30),
                unitPriceWon = 324.4,
                amountInput = "12976",
            ),
            onEvent = {},
        )
    }
}
