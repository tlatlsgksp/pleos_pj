package com.example.pleos.domain.repository

import com.example.pleos.domain.model.DataSourceMode
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

    companion object {
        /** [기본값] Public fast-charger tariff ballpark. The user can change it. */
        const val DEFAULT_CHARGE_UNIT_PRICE_WON = 324.4
    }
}
