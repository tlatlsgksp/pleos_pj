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
    onOpenCare: () -> Unit,
    onOpenCharge: (recordId: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
                DashboardEffect.NavigateToCare -> onOpenCare()
                is DashboardEffect.NavigateToCharge -> onOpenCharge(effect.recordId)
            }
        }
    }

    DashboardScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}
