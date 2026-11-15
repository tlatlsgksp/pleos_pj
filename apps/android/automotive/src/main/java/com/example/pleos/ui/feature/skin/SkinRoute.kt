package com.example.pleos.ui.feature.skin

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SkinRoute(onBack: () -> Unit, viewModel: SkinViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SkinEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    SkinScreen(onBack = onBack, onAmbientColor = viewModel::onAmbientColor, snackbarHostState = snackbarHostState)
}
