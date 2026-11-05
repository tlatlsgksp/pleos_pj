package com.example.pleos.ui.feature.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.theme.PleosTheme
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun CareScreen(
    state: CareState,
    onEvent: (CareEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEvent(CareEvent.BackTapped) }) {
                    Text(stringResource(R.string.common_back), style = MaterialTheme.typography.labelLarge)
                }
                Text(stringResource(R.string.care_title), style = MaterialTheme.typography.headlineMedium)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.items, key = { it.id }) { item ->
                    CareItemCard(
                        item = item,
                        saving = state.savingItemId == item.id,
                        onServiced = { onEvent(CareEvent.ServicedTapped(item.id)) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun CareItemCard(item: CareItemUi, saving: Boolean, onServiced: (() -> Unit)?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(item.status.label(), style = MaterialTheme.typography.labelLarge, color = item.status.color())
            }
            if (item.ratio != null) {
                LinearProgressIndicator(
                    progress = { item.ratio.toFloat().coerceIn(0f, 1f) },
                    color = item.status.color(),
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                )
            }
            Text(item.detailText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onServiced != null) {
                OutlinedButton(onClick = onServiced, enabled = !saving) {
                    Text(stringResource(R.string.care_serviced_now), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CareItemUi.detailText(): String {
    if (status == CareStatus.UNKNOWN) return stringResource(R.string.care_unknown_hint)
    val parts = buildList {
        remainingKm?.let {
            add(
                if (it >= 0) stringResource(R.string.care_remaining_km, Format.km(it))
                else stringResource(R.string.care_over_km, Format.km(abs(it))),
            )
        }
        dueDate?.let { add(stringResource(R.string.care_due_date, Format.date(it))) }
    }
    return parts.joinToString(" · ")
}

@Composable
internal fun CareStatus.label(): String = stringResource(
    when (this) {
        CareStatus.OK -> R.string.care_status_ok
        CareStatus.DUE_SOON -> R.string.care_status_due_soon
        CareStatus.OVERDUE -> R.string.care_status_overdue
        CareStatus.UNKNOWN -> R.string.care_status_unknown
    },
)

@Composable
internal fun CareStatus.color(): Color = when (this) {
    CareStatus.OK -> MaterialTheme.colorScheme.tertiary
    CareStatus.DUE_SOON -> MaterialTheme.colorScheme.secondary
    CareStatus.OVERDUE -> MaterialTheme.colorScheme.error
    CareStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 900)
@Composable
private fun CareScreenPreview() {
    PleosTheme {
        CareScreen(
            state = CareState(
                isLoading = false,
                items = listOf(
                    CareItemUi("tire_rotation", "타이어 위치교환", CareStatus.OVERDUE, 1.2, -2_000.0, LocalDate.of(2026, 10, 1), 10_000, 6),
                    CareItemUi("cabin_filter", "에어컨 필터", CareStatus.DUE_SOON, 0.85, 2_250.0, LocalDate.of(2027, 3, 1), 15_000, 12),
                    CareItemUi("wiper", "와이퍼", CareStatus.UNKNOWN, null, null, null, null, 12),
                ),
            ),
            onEvent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
