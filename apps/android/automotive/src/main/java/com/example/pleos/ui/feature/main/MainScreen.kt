package com.example.pleos.ui.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.pleos.R
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.ui.design.BoxTabs
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.StatusLabel
import com.example.pleos.ui.feature.dashboard.DashboardRoute
import com.example.pleos.ui.feature.ledger.LedgerRoute
import com.example.pleos.ui.feature.mycar.MyCarRoute
import com.example.pleos.ui.theme.Pleos
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Top-level sections, in order of how often they are used while driving. */
enum class MainTab { HOME, LEDGER, MY_CAR }

@HiltViewModel
class MainViewModel @Inject constructor(gateway: VehicleGateway) : ViewModel() {
    /** The header says so whenever the screens show the demo, not the car. */
    val isDemo: StateFlow<Boolean> = gateway.activeSource
        .map { it == DataSourceMode.DEMO }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

/**
 * The app's first depth (docs/visual/ia-flow-layout.html): Page Navigation (Main type) and fitted
 * Box tabs. Pleos draws the GNB itself as system UI, so the app has no bottom bar of its own.
 */
@Composable
fun MainRoute(
    onOpenCharge: (recordId: String) -> Unit,
    onOpenSkin: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val isDemo by viewModel.isDemo.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    val labels = listOf(
        stringResource(R.string.tab_home),
        stringResource(R.string.tab_ledger),
        stringResource(R.string.tab_my_car),
    )
    Scaffold(containerColor = Pleos.colors.background) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(horizontal = Space.l)) {
                PageNavigation(title = stringResource(R.string.app_title)) {
                    if (isDemo) StatusLabel(stringResource(R.string.dashboard_demo_badge), Pleos.colors.textSecondary)
                }
                BoxTabs(labels, tab, { tab = it })
                Spacer(Modifier.height(Space.m))
            }
            when (MainTab.entries[tab]) {
                MainTab.HOME -> DashboardRoute(onOpenLedger = { tab = MainTab.LEDGER.ordinal }, onOpenCharge = onOpenCharge)
                MainTab.LEDGER -> LedgerRoute()
                MainTab.MY_CAR -> MyCarRoute(onOpenSkin = onOpenSkin)
            }
        }
    }
}
