package com.example.pleos.ui.feature.charge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.ButtonSize
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.CardGap
import com.example.pleos.ui.design.CenteredMessage
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosCard
import com.example.pleos.ui.design.ScreenPadding
import com.example.pleos.ui.design.StatusLabel
import com.example.pleos.ui.design.TWO_PANE_MIN_WIDTH_DP
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

/** What the car measured on the left, what the driver confirms on the right; stacked in portrait. */
@Composable
fun ChargeConfirmScreen(state: ChargeConfirmState, onEvent: (ChargeConfirmEvent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = Pleos.colors.background) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            val twoPane = maxWidth.value >= TWO_PANE_MIN_WIDTH_DP
            Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
                PageNavigation(title = stringResource(R.string.charge_title), onBack = { onEvent(ChargeConfirmEvent.BackTapped) })
                Box(Modifier.fillMaxSize()) {
                    when {
                        state.isLoading -> Unit
                        state.isMissing -> CenteredMessage(stringResource(R.string.charge_missing))
                        twoPane -> Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
                            MeasuredCard(state, Modifier.weight(1f))
                            AmountCard(state, onEvent, Modifier.weight(1f))
                        }
                        else -> Column(
                            verticalArrangement = Arrangement.spacedBy(CardGap),
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = ScreenPadding),
                        ) {
                            MeasuredCard(state, Modifier.fillMaxWidth())
                            AmountCard(state, onEvent, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasuredCard(state: ChargeConfirmState, modifier: Modifier) {
    val c = Pleos.colors
    PleosCard(modifier) {
        StatusLabel(stringResource(R.string.charge_vehicle_checked), c.active)
        Text(
            text = state.amountKwh?.let(Format::kwh) ?: stringResource(R.string.dashboard_value_unknown),
            style = MaterialTheme.typography.displaySmall,
            color = c.textPrimary,
        )
        if (state.levelBefore != null && state.levelAfter != null) {
            Text(
                stringResource(R.string.charge_level, Format.percent(state.levelBefore), Format.percent(state.levelAfter)),
                style = MaterialTheme.typography.bodyLarge,
                color = c.textPrimary,
            )
        }
        val meta = listOfNotNull(state.occurredAt?.format(timeFormat), state.odometerKm?.let(Format::km))
        Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
    }
}

@Composable
private fun AmountCard(state: ChargeConfirmState, onEvent: (ChargeConfirmEvent) -> Unit, modifier: Modifier) {
    val c = Pleos.colors
    PleosCard(modifier, verticalArrangement = Arrangement.spacedBy(CardGap)) {
        Text(
            stringResource(R.string.charge_amount_label),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = c.textPrimary,
        )
        // Text Field: pill shape, dark border when focused (UI_DESIGN_GUIDE 8).
        OutlinedTextField(
            value = state.amountInput,
            onValueChange = { onEvent(ChargeConfirmEvent.AmountChanged(it)) },
            suffix = { Text("원", style = MaterialTheme.typography.titleSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(50),
            textStyle = MaterialTheme.typography.titleSmall.copy(color = c.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.buttonFilled,
                unfocusedBorderColor = c.outline,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        state.unitPriceWon?.let {
            Text(
                stringResource(R.string.charge_estimate_hint, "%.1f".format(it)),
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
            )
        }
        // Confirm on the left in the dark fill, the alternative on the right.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            PleosButton(
                text = stringResource(R.string.charge_confirm),
                onClick = { onEvent(ChargeConfirmEvent.ConfirmTapped) },
                style = ButtonStyle.Filled,
                size = ButtonSize.Large,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
            PleosButton(
                text = stringResource(R.string.charge_dismiss),
                onClick = { onEvent(ChargeConfirmEvent.DismissTapped) },
                size = ButtonSize.Large,
                enabled = !state.isSaving,
            )
        }
    }
}

private val previewState = ChargeConfirmState(
    isLoading = false,
    amountKwh = 40.0,
    levelBefore = 17.4,
    levelAfter = 69.1,
    odometerKm = 12_370.0,
    occurredAt = LocalDateTime.of(2026, 11, 5, 18, 30),
    unitPriceWon = 324.4,
    amountInput = "12976",
)

@Preview(name = "Landscape window", showBackground = true, widthDp = 1700, heightDp = 1184)
@Composable
private fun ChargeConfirmLandscapePreview() {
    PleosTheme { ChargeConfirmScreen(previewState, {}) }
}

@Preview(name = "Narrowest portrait window", showBackground = true, widthDp = 628, heightDp = 855)
@Composable
private fun ChargeConfirmPortraitPreview() {
    PleosTheme { ChargeConfirmScreen(previewState, {}) }
}
