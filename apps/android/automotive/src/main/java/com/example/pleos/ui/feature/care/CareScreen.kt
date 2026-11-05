package com.example.pleos.ui.feature.care

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.ButtonSize
import com.example.pleos.ui.design.CardGap
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosCard
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.ScreenPadding
import com.example.pleos.ui.design.StatusLabel
import com.example.pleos.ui.design.TWO_PANE_MIN_WIDTH_DP
import com.example.pleos.ui.theme.Pleos
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Pleos.colors.background,
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            // Two columns in a landscape window, one in portrait (UI_DESIGN_GUIDE 2).
            val columns = if (maxWidth.value >= TWO_PANE_MIN_WIDTH_DP) 2 else 1
            Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
                PageNavigation(
                    title = stringResource(R.string.care_title),
                    onBack = { onEvent(CareEvent.BackTapped) },
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                    verticalArrangement = Arrangement.spacedBy(CardGap),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = ScreenPadding),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        CareItemCard(
                            item = item,
                            saving = state.savingItemId == item.id,
                            onServiced = { onEvent(CareEvent.ServicedTapped(item.id)) },
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CareItemCard(item: CareItemUi, saving: Boolean, onServiced: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    PleosCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            StatusLabel(item.status.label(), item.status.color())
        }
        if (item.ratio != null) ProgressTrack(item.ratio.toFloat(), item.status.color())
        Text(item.detailText(), style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        if (onServiced != null) {
            PleosButton(
                text = stringResource(R.string.care_serviced_now),
                onClick = onServiced,
                size = ButtonSize.Small,
                enabled = !saving,
            )
        }
    }
}

/** A rounded progress bar; past 1.0 it stays full and the color says overdue. */
@Composable
private fun ProgressTrack(ratio: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Pleos.colors.surfaceHigh),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
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

/** informative_active / positive / negative from the guide; never the regulation (telltale) colors. */
@Composable
internal fun CareStatus.color(): Color = when (this) {
    CareStatus.OK -> Pleos.colors.active
    CareStatus.DUE_SOON -> Pleos.colors.positive
    CareStatus.OVERDUE -> Pleos.colors.negative
    CareStatus.UNKNOWN -> Pleos.colors.textTertiary
}

private val previewItems = listOf(
    CareItemUi("tire_rotation", "타이어 위치교환", CareStatus.OVERDUE, 1.2, -2_000.0, LocalDate.of(2026, 10, 1), 10_000, 6),
    CareItemUi("cabin_filter", "에어컨 필터", CareStatus.DUE_SOON, 0.85, 2_250.0, LocalDate.of(2027, 3, 1), 15_000, 12),
    CareItemUi("brake_fluid", "브레이크 오일", CareStatus.OK, 0.3, 21_000.0, LocalDate.of(2028, 1, 1), 30_000, 24),
    CareItemUi("wiper", "와이퍼", CareStatus.UNKNOWN, null, null, null, null, 12),
)

@Preview(name = "Landscape window", showBackground = true, widthDp = 1700, heightDp = 1184)
@Composable
private fun CareScreenLandscapePreview() {
    PleosTheme { CareScreen(CareState(previewItems, isLoading = false), {}, SnackbarHostState()) }
}

@Preview(name = "Narrowest portrait window", showBackground = true, widthDp = 628, heightDp = 855)
@Composable
private fun CareScreenPortraitPreview() {
    PleosTheme { CareScreen(CareState(previewItems, isLoading = false), {}, SnackbarHostState()) }
}
