package com.example.pleos.domain.model

import com.example.pleos.domain.model.EnergyType.EV
import com.example.pleos.domain.model.EnergyType.FUEL

/**
 * Charging and fuel station brands a driver can prefer. EV owners carry several membership cards,
 * so a preferred brand is offered first when one is in reach.
 */
enum class StationBrand(val label: String, val energyType: EnergyType) {
    EPIT("E-pit", EV),
    MINISTRY_OF_ENVIRONMENT("환경부", EV),
    KEPCO("한국전력", EV),
    SK_ELECLINK("SK일렉링크", EV),
    GS_CHARGEV("GS차지비", EV),
    EVERON("에버온", EV),
    CHAEVI("채비", EV),
    WATER("워터", EV),

    SK_ENERGY("SK에너지", FUEL),
    GS_CALTEX("GS칼텍스", FUEL),
    S_OIL("S-OIL", FUEL),
    HD_OILBANK("HD현대오일뱅크", FUEL),
    ALTTEUL("알뜰주유소", FUEL),
    ;

    companion object {
        fun forEnergyType(energyType: EnergyType): List<StationBrand> = entries.filter { it.energyType == energyType }
    }
}
