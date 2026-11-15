package com.example.pleos.domain.repository

import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.StationBrand
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val dataSourceMode: Flow<DataSourceMode>

    suspend fun setDataSourceMode(mode: DataSourceMode)

    /** Whether the user agreed to send pseudonymized records to the server. Nothing is uploaded before this. */
    val consentGiven: Flow<Boolean>

    suspend fun setConsentGiven(given: Boolean)

    /** Won per kWh used for a detected charge when the charger's price is unknown (FEATURE_LOGIC L2-4). */
    val chargeUnitPriceWon: Flow<Double>

    suspend fun setChargeUnitPriceWon(won: Double)

    /** Charging and fuel brands the driver holds cards for; offered first when in reach. */
    val preferredBrands: Flow<Set<StationBrand>>

    suspend fun setPreferredBrands(brands: Set<StationBrand>)

    /** Eco driving guide: mood lights turn orange or red on wasteful driving. Off until the driver turns it on. */
    val ecoGuideEnabled: Flow<Boolean>

    suspend fun setEcoGuideEnabled(enabled: Boolean)

    /** The driver's own mood light color (0xRRGGBB), picked in the car skin; the eco guide returns to it. */
    val ambientBaseColor: Flow<Int>

    suspend fun setAmbientBaseColor(rgb: Int)

    companion object {
        /** [기본값] Public fast-charger tariff ballpark. The user can change it. */
        const val DEFAULT_CHARGE_UNIT_PRICE_WON = 324.4

        /** The emulator's cabin lights read back white before anything is set. */
        const val DEFAULT_AMBIENT_COLOR = 0xFFFFFF
    }
}
