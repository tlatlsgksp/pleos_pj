package com.example.pleos.ui.feature.skin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.pleos.R
import com.example.pleos.ui.design.AdaptiveScreen
import com.example.pleos.ui.design.BigSwatch
import com.example.pleos.ui.design.ButtonSize
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.CarModel
import com.example.pleos.ui.design.HomeCard
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.WindowWidth
import com.example.pleos.ui.theme.Pleos

/**
 * Car skin studio (docs/visual/wireframes.html F). The car fills a card and takes every pick at
 * once; the paints and mood lights sit beside it in the two-thirds window, below it in the
 * one-third window. "저장" stays at the bottom and lights only when something changed.
 */
@Composable
fun SkinScreen(
    state: SkinState,
    onEvent: (SkinEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Pleos.colors.background,
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            PageNavigation(
                title = stringResource(R.string.skin_title),
                onBack = onBack,
                modifier = Modifier.padding(horizontal = Space.l),
            )
            AdaptiveScreen(Modifier.padding(bottom = Space.l)) { window ->
                if (window.width == WindowWidth.TWO) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        CarCard(state, Modifier.weight(1.3f).fillMaxHeight())
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Choices(state, onEvent, Modifier.weight(1f).verticalScroll(rememberScrollState()))
                            SaveBar(state, onEvent)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                            CarCard(state, Modifier.fillMaxWidth().aspectRatio(1.5f))
                            Choices(state, onEvent, Modifier.fillMaxWidth())
                        }
                        SaveBar(state, onEvent)
                    }
                }
            }
        }
    }
}

/** The customised car on its white card; every pick redraws it. */
@Composable
private fun CarCard(state: SkinState, modifier: Modifier) {
    HomeCard(modifier) {
        CarModel(paint = state.paint, ambient = state.ambient, modifier = Modifier.fillMaxSize())
    }
}

/** Paint, then mood light: big swatches with the factory name under each paint. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(state: SkinState, onEvent: (SkinEvent) -> Unit, modifier: Modifier) {
    val c = Pleos.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(stringResource(R.string.skin_paint), style = PleosText.sectionTitle, color = c.textPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SkinViewModel.PAINTS.forEach { paint ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        BigSwatch(paint.rgb, selected = paint.rgb == state.paint, onClick = { onEvent(SkinEvent.PaintPicked(paint.rgb)) })
                        Text(paint.name, style = PleosText.label, color = if (paint.rgb == state.paint) c.textPrimary else c.textSecondary, maxLines = 1)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(stringResource(R.string.ambient_title), style = PleosText.sectionTitle, color = c.textPrimary)
            Text(stringResource(R.string.ambient_hint), style = PleosText.body, color = c.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SkinViewModel.AMBIENTS.forEach { rgb ->
                    BigSwatch(rgb, selected = rgb == state.ambient, onClick = { onEvent(SkinEvent.AmbientPicked(rgb)) })
                }
            }
        }
    }
}

/** Fixed at the bottom of the choices: what is pending, and the one button that keeps it. */
@Composable
private fun SaveBar(state: SkinState, onEvent: (SkinEvent) -> Unit) {
    val c = Pleos.colors
    Row(
        Modifier.fillMaxWidth().padding(top = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        // The driver sits to the left of the window, so the action comes first.
        PleosButton(
            stringResource(R.string.me_save),
            onClick = { onEvent(SkinEvent.SaveTapped) },
            style = ButtonStyle.Filled,
            size = ButtonSize.Large,
            enabled = state.dirty && !state.isSaving,
        )
        Text(
            stringResource(if (state.dirty) R.string.skin_unsaved else R.string.skin_saved),
            style = PleosText.body,
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}
