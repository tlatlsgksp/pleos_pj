package com.example.pleos.platform.demo

import com.example.pleos.domain.gateway.ChargerSource
import com.example.pleos.domain.model.ChargerCandidate
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.StationBrand
import com.example.pleos.domain.route.ChargeStopRules

/**
 * Demo charger list until the backend serves the public charger API (FEATURE_LOGIC L4-2).
 * These are not real stations: each is a landmark coordinate with a "시연용" name, and every
 * candidate is flagged isDemoData so the screen says so (research notes 13).
 */
internal class BundledChargerSource : ChargerSource {

    override suspend fun nearby(latitude: Double, longitude: Double, limit: Int): List<ChargerCandidate> =
        PLACES
            .map { (place, brand) ->
                ChargerCandidate(place, ChargeStopRules.distanceKm(latitude, longitude, place.latitude, place.longitude), isDemoData = true, brand = brand)
            }
            .sortedBy { it.distanceKm }
            .take(limit)

    private companion object {
        // Brands are assigned for the demo, not taken from the real operators. Pangyo has two
        // stations of different brands so a brand preference visibly changes the offer.
        val PLACES = listOf(
            Place("시연용 충전소 · 서울역", 37.5547, 126.9707) to StationBrand.MINISTRY_OF_ENVIRONMENT,
            Place("시연용 충전소 · 강남역", 37.4979, 127.0276) to StationBrand.EPIT,
            Place("시연용 충전소 · 여의도", 37.5219, 126.9245) to StationBrand.KEPCO,
            Place("시연용 충전소 · 잠실", 37.5133, 127.1001) to StationBrand.SK_ELECLINK,
            Place("시연용 충전소 · 판교", 37.3948, 127.1112) to StationBrand.CHAEVI,
            Place("시연용 충전소 · 판교테크노밸리", 37.4020, 127.1086) to StationBrand.EPIT,
            Place("시연용 충전소 · 수원", 37.2659, 127.0001) to StationBrand.GS_CHARGEV,
            Place("시연용 충전소 · 천안", 36.8151, 127.1139) to StationBrand.EPIT,
            Place("시연용 충전소 · 대전", 36.3504, 127.3845) to StationBrand.EVERON,
        )
    }
}
