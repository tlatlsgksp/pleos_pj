package com.example.pleos.ui.feature.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pleos.R
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.StationBrand
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosCard
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme

/** Preferred brands as the detail of the my car tab; wires its own ViewModel. */
@Composable
fun BrandPickerRoute(modifier: Modifier = Modifier, viewModel: BrandViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BrandPicker(state, viewModel::onEvent, modifier)
}

/** Preferred charging and fuel brands. A selected brand is a filled button; tapping toggles it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandPicker(state: BrandState, onEvent: (BrandEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Text(stringResource(R.string.brand_hint), style = PleosText.body, color = c.textSecondary)
        state.groups.forEach { group ->
            PleosCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (group.energyType == EnergyType.EV) R.string.brand_group_ev else R.string.brand_group_fuel),
                    style = PleosText.cardTitle,
                    color = c.textPrimary,
                )
                if (group.energyType == EnergyType.FUEL) {
                    Text(stringResource(R.string.brand_fuel_pending), style = PleosText.label, color = c.textTertiary)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    group.brands.forEach { brand ->
                        val isSelected = brand in state.selected
                        PleosButton(
                            text = brand.label,
                            onClick = { onEvent(BrandEvent.BrandTapped(brand)) },
                            style = if (isSelected) ButtonStyle.Filled else ButtonStyle.Basic,
                            modifier = Modifier.semantics { selected = isSelected },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Brand picker", showBackground = true, widthDp = 1100, heightDp = 900)
@Composable
private fun BrandPickerPreview() {
    PleosTheme {
        BrandPicker(
            BrandState(groups = groupsFor(null), selected = setOf(StationBrand.EPIT, StationBrand.KEPCO, StationBrand.SK_ENERGY), isLoading = false),
            {},
        )
    }
}
