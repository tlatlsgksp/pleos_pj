package com.example.pleos.ui.feature.mycar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.pleos.R
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.ui.common.Format
import com.example.pleos.ui.design.AdaptiveScreen
import com.example.pleos.ui.design.ButtonStyle
import com.example.pleos.ui.design.ColorSwatch
import com.example.pleos.ui.design.EmptyDetail
import com.example.pleos.ui.design.FactRow
import com.example.pleos.ui.design.ListCard
import com.example.pleos.ui.design.ListDivider
import com.example.pleos.ui.design.ListRow
import com.example.pleos.ui.design.PaneColumn
import com.example.pleos.ui.design.PleosBackLink
import com.example.pleos.ui.design.PleosButton
import com.example.pleos.ui.design.PleosCard
import com.example.pleos.ui.design.PleosSwitch
import com.example.pleos.ui.design.PleosText
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.Section
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.design.TwoColumns
import com.example.pleos.ui.design.UNKNOWN
import com.example.pleos.ui.design.WindowWidth
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.PleosTheme

/**
 * My car tab: personal settings (docs/visual/layout-variants.html, my car B). In the two-thirds
 * window the list takes a third and the selected setting opens on the right; in the one-third
 * window the detail replaces the list, with a back link.
 */
@Composable
fun MyCarScreen(
    state: MyCarState,
    onEvent: (MyCarEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    brandPicker: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Pleos.colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        AdaptiveScreen(Modifier.padding(innerPadding)) { window ->
            when (window.width) {
                WindowWidth.TWO -> TwoColumns(
                    leftWeight = 1f,
                    rightWeight = 2f,
                    left = { SettingsList(state, onEvent) },
                    right = {
                        val section = state.selected
                        Section(section?.title() ?: stringResource(R.string.ledger_detail), first = true) {
                            if (section == null) EmptyDetail(stringResource(R.string.mycar_pick)) else Detail(section, state, onEvent, brandPicker)
                        }
                    },
                )
                WindowWidth.ONE -> PaneColumn {
                    val section = state.selected
                    if (section == null) {
                        SettingsList(state, onEvent)
                    } else {
                        PleosBackLink(stringResource(R.string.mycar_back), { onEvent(MyCarEvent.BackTapped) })
                        Section(section.title(), first = true) { Detail(section, state, onEvent, brandPicker) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyCarSection.title(): String = stringResource(
    when (this) {
        MyCarSection.SKIN -> R.string.skin_title
        MyCarSection.ECO -> R.string.eco_title
        MyCarSection.BRANDS -> R.string.brand_title
        MyCarSection.PRICE -> R.string.mycar_price
        MyCarSection.CONSENT -> R.string.mycar_consent
        MyCarSection.DEMO -> R.string.mycar_demo
        MyCarSection.VEHICLE -> R.string.mycar_vehicle
    },
)

@Composable
private fun SettingsList(state: MyCarState, onEvent: (MyCarEvent) -> Unit) {
    fun select(section: MyCarSection) = { onEvent(MyCarEvent.SectionTapped(section)) }
    Section(stringResource(R.string.tab_my_car), first = true) {
        ListCard {
            ListRow(
                title = stringResource(R.string.skin_title),
                detail = stringResource(R.string.mycar_ambient_base, "#%06X".format(state.ambientColor)),
                selected = state.selected == MyCarSection.SKIN,
                onClick = select(MyCarSection.SKIN),
            ) { ColorSwatch(state.ambientColor) }
            ListDivider()
            ListRow(
                title = stringResource(R.string.mycar_vehicle),
                detail = vehicleSummary(state),
                selected = state.selected == MyCarSection.VEHICLE,
                onClick = select(MyCarSection.VEHICLE),
            )
        }
    }
    Section(stringResource(R.string.mycar_settings)) {
        ListCard {
            ListRow(
                title = stringResource(R.string.eco_title),
                detail = stringResource(if (state.ecoGuideEnabled) R.string.mycar_on else R.string.mycar_off),
                selected = state.selected == MyCarSection.ECO,
                onClick = select(MyCarSection.ECO),
            ) { PleosSwitch(state.ecoGuideEnabled, { onEvent(MyCarEvent.EcoGuideSwitched(it)) }) }
            ListDivider()
            ListRow(
                title = stringResource(R.string.brand_title),
                detail = state.preferredBrands.joinToString(", ").ifEmpty { stringResource(R.string.mycar_brands_none) },
                selected = state.selected == MyCarSection.BRANDS,
                onClick = select(MyCarSection.BRANDS),
            )
            ListDivider()
            ListRow(
                title = stringResource(R.string.mycar_price),
                detail = stringResource(R.string.mycar_price_short, "%.1f".format(state.chargeUnitPriceWon)),
                selected = state.selected == MyCarSection.PRICE,
                onClick = select(MyCarSection.PRICE),
            )
            ListDivider()
            ListRow(
                title = stringResource(R.string.mycar_consent),
                detail = stringResource(if (state.consentGiven) R.string.mycar_on else R.string.mycar_off),
                selected = state.selected == MyCarSection.CONSENT,
                onClick = select(MyCarSection.CONSENT),
            ) { PleosSwitch(state.consentGiven, { onEvent(MyCarEvent.ConsentSwitched(it)) }) }
            ListDivider()
            ListRow(
                title = stringResource(R.string.mycar_demo),
                detail = stringResource(if (state.isDemo) R.string.mycar_demo_on else R.string.mycar_demo_off),
                selected = state.selected == MyCarSection.DEMO,
                onClick = select(MyCarSection.DEMO),
            )
        }
    }
}

@Composable
private fun vehicleSummary(state: MyCarState): String {
    val type = when (state.energyType) {
        EnergyType.EV -> stringResource(R.string.mycar_type_ev)
        EnergyType.FUEL -> stringResource(R.string.mycar_type_fuel)
        null -> UNKNOWN
    }
    val capacity = state.capacity?.let { if (state.energyType == EnergyType.EV) Format.kwh(it) else Format.liters(it) } ?: UNKNOWN
    return stringResource(R.string.mycar_vehicle_body, type, capacity)
}

@Composable
private fun Detail(section: MyCarSection, state: MyCarState, onEvent: (MyCarEvent) -> Unit, brandPicker: @Composable () -> Unit) {
    val c = Pleos.colors
    when (section) {
        MyCarSection.SKIN -> PleosCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mycar_skin_hint), style = PleosText.body, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                ColorSwatch(state.ambientColor)
                Text(stringResource(R.string.mycar_ambient_base, "#%06X".format(state.ambientColor)), style = PleosText.body, color = c.textPrimary, modifier = Modifier.weight(1f))
            }
            PleosButton(stringResource(R.string.mycar_open_skin), { onEvent(MyCarEvent.OpenSkinTapped) }, style = ButtonStyle.Filled)
        }
        MyCarSection.ECO -> PleosCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(stringResource(R.string.eco_off_hint), style = PleosText.body, color = c.textSecondary, modifier = Modifier.weight(1f))
                PleosSwitch(state.ecoGuideEnabled, { onEvent(MyCarEvent.EcoGuideSwitched(it)) })
            }
            FactRow(stringResource(R.string.eco_caution_rule), stringResource(R.string.eco_light_caution))
            FactRow(stringResource(R.string.eco_bad_rule), stringResource(R.string.eco_light_bad))
            FactRow(stringResource(R.string.eco_good_rule), stringResource(R.string.eco_light_base))
        }
        MyCarSection.BRANDS -> brandPicker()
        MyCarSection.PRICE -> PleosCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mycar_price_body), style = PleosText.body, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(stringResource(R.string.mycar_price_value, "%.1f".format(state.chargeUnitPriceWon)), style = PleosText.value, color = c.textPrimary, modifier = Modifier.weight(1f))
                PleosButton("−", { onEvent(MyCarEvent.PriceStepped(up = false)) })
                PleosButton("+", { onEvent(MyCarEvent.PriceStepped(up = true)) })
            }
        }
        MyCarSection.CONSENT -> PleosCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(stringResource(R.string.mycar_consent_body), style = PleosText.body, color = c.textSecondary, modifier = Modifier.weight(1f))
                PleosSwitch(state.consentGiven, { onEvent(MyCarEvent.ConsentSwitched(it)) })
            }
        }
        MyCarSection.DEMO -> PleosCard(Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isDemo) R.string.mycar_demo_on else R.string.mycar_demo_off), style = PleosText.body, color = c.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                if (state.isDemo) {
                    PleosButton(
                        stringResource(if (state.isDemoPlaying) R.string.dashboard_demo_stop else R.string.dashboard_demo_play),
                        { onEvent(if (state.isDemoPlaying) MyCarEvent.StopDemoTapped else MyCarEvent.PlayDemoTapped) },
                        style = ButtonStyle.Filled,
                    )
                }
                PleosButton(
                    stringResource(if (state.isDemo) R.string.dashboard_use_real else R.string.dashboard_use_demo),
                    { onEvent(MyCarEvent.SourceToggleTapped) },
                )
            }
        }
        MyCarSection.VEHICLE -> PleosCard(Modifier.fillMaxWidth()) {
            FactRow(stringResource(R.string.mycar_fact_type), vehicleSummary(state))
            FactRow(stringResource(R.string.mycar_fact_vin), stringResource(if (state.hasVin) R.string.mycar_vin_yes else R.string.mycar_vin_no))
        }
    }
}

private val previewState = MyCarState(
    energyType = EnergyType.EV,
    capacity = 77.4,
    ambientColor = 0x2979FF,
    ecoGuideEnabled = true,
    preferredBrands = listOf("E-pit", "한국전력"),
    isDemo = true,
    selected = MyCarSection.ECO,
)

@Preview(name = "Two thirds", showBackground = true, widthDp = 1700, heightDp = 1000)
@Composable
private fun MyCarTwoPreview() {
    PleosTheme { MyCarScreen(previewState, {}, SnackbarHostState()) }
}

@Preview(name = "One third, list", showBackground = true, widthDp = 628, heightDp = 700)
@Composable
private fun MyCarOnePreview() {
    PleosTheme { MyCarScreen(previewState.copy(selected = null), {}, SnackbarHostState()) }
}
