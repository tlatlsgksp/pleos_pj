package com.example.pleos.ui.feature.riding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RidingRoute(onBack: () -> Unit, viewModel: RidingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : RidingActions {
            override fun openRoom(id: String) = viewModel.openRoom(id)
            override fun openList() = viewModel.openList()
            override fun openCreate() = viewModel.openCreate()
            override fun toggleInvitee(id: String) = viewModel.toggleInvitee(id)
            override fun create() { viewModel.create() }
            override fun join(id: String) { viewModel.join(id) }
            override fun leave(id: String) { viewModel.leave(id) }
            override fun decline(id: String) { viewModel.decline(id) }
            override fun setTalking(talking: Boolean) = viewModel.setTalking(talking)
            override fun toggleSound() = viewModel.toggleSound()
        }
    }
    RidingScreen(state, onBack, actions)
}
