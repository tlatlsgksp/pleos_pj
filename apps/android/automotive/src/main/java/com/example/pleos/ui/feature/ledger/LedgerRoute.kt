package com.example.pleos.ui.feature.ledger

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The fuel and charge tab: statistics and records. */
@Composable
fun LedgerRoute(
    viewModel: LedgerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LedgerEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    LedgerScreen(state, viewModel::onEvent, snackbarHostState)
}
