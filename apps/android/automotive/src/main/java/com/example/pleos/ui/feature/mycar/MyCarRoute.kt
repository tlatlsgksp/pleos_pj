package com.example.pleos.ui.feature.mycar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pleos.ui.feature.brand.BrandPickerRoute

@Composable
fun MyCarRoute(
    onOpenSkin: () -> Unit,
    viewModel: MyCarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyCarEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
                MyCarEffect.NavigateToSkin -> onOpenSkin()
            }
        }
    }

    MyCarScreen(state, viewModel::onEvent, snackbarHostState, brandPicker = { BrandPickerRoute() })
}
