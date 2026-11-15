package com.example.pleos.ui.feature.charge

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChargeConfirmRoute(onClose: () -> Unit, viewModel: ChargeConfirmViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // A toast outlives this screen, which closes right after confirming.
                is ChargeConfirmEffect.ShowMessage -> Toast.makeText(context, effect.text, Toast.LENGTH_SHORT).show()
                ChargeConfirmEffect.Close -> onClose()
            }
        }
    }

    ChargeConfirmScreen(state = state, onEvent = viewModel::onEvent)
}
