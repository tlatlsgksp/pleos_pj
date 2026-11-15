package com.example.pleos.ui.feature.dashboard

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires the ViewModel to the stateless [DashboardScreen] and consumes one-shot effects. */
@Composable
fun DashboardRoute(
    onOpenCharge: (recordId: String) -> Unit,
    onOpenRiding: (ridingId: String?) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
                is DashboardEffect.NavigateToCharge -> onOpenCharge(effect.recordId)
                is DashboardEffect.OpenRiding -> onOpenRiding(effect.ridingId)
                DashboardEffect.OpenMap -> onOpenMap()
            }
        }
    }

    DashboardScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}
