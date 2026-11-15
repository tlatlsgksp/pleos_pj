package com.example.pleos.ui.feature.mycar

import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.repository.SettingsRepository

/** Rows of the my car list; the selected one opens in the detail column. */
enum class MyCarSection { SKIN, ECO, BRANDS, PRICE, CONSENT, DEMO, VEHICLE }

data class MyCarState(
    val energyType: EnergyType? = null,
    /** kWh for an EV, L for a fuel car. */
    val capacity: Double? = null,
    val hasVin: Boolean = false,
    /** The driver's mood light color (0xRRGGBB) from the skin. */
    val ambientColor: Int = SettingsRepository.DEFAULT_AMBIENT_COLOR,
    val ecoGuideEnabled: Boolean = false,
    val preferredBrands: List<String> = emptyList(),
    val chargeUnitPriceWon: Double = SettingsRepository.DEFAULT_CHARGE_UNIT_PRICE_WON,
    val consentGiven: Boolean = false,
    val isDemo: Boolean = false,
    val isDemoPlaying: Boolean = false,
    val selected: MyCarSection? = null,
)

sealed interface MyCarEvent {
    data class SectionTapped(val section: MyCarSection) : MyCarEvent
    /** Back from a detail shown in place of the list (one-third window). */
    data object BackTapped : MyCarEvent
    data object OpenSkinTapped : MyCarEvent
    data class EcoGuideSwitched(val enabled: Boolean) : MyCarEvent
    data class PriceStepped(val up: Boolean) : MyCarEvent
    data class ConsentSwitched(val given: Boolean) : MyCarEvent
    data object SourceToggleTapped : MyCarEvent
    data object PlayDemoTapped : MyCarEvent
    data object StopDemoTapped : MyCarEvent

    data class Changed(val transform: (MyCarState) -> MyCarState) : MyCarEvent
}

sealed interface MyCarEffect {
    data class ShowMessage(val text: String) : MyCarEffect
    data object NavigateToSkin : MyCarEffect
}
