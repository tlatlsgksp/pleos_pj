package com.example.pleos.ui.feature.care

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CareRoute(onBack: () -> Unit, viewModel: CareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CareEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
                CareEffect.NavigateBack -> onBack()
            }
        }
    }

    CareScreen(state = state, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}
