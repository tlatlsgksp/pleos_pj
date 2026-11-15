package com.example.pleos.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.StationBrand
import com.example.pleos.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val dataSourceMode = stringPreferencesKey("data_source_mode")
        val consentGiven = booleanPreferencesKey("consent_given")
        val chargeUnitPriceWon = doublePreferencesKey("charge_unit_price_won")
        val preferredBrands = stringSetPreferencesKey("preferred_brands")
        val ecoGuideEnabled = booleanPreferencesKey("eco_guide_enabled")
        val ambientBaseColor = intPreferencesKey("ambient_base_color")
    }

    // REAL by default. Where no Pleos vehicle service exists, the switching gateway falls back to
    // the demo and reports DEMO, so a phone or plain emulator still shows the demo.
    override val dataSourceMode: Flow<DataSourceMode> = dataStore.data.map { prefs ->
        prefs[Keys.dataSourceMode]?.let { runCatching { DataSourceMode.valueOf(it) }.getOrNull() }
            ?: DataSourceMode.REAL
    }

    override suspend fun setDataSourceMode(mode: DataSourceMode) {
        dataStore.edit { it[Keys.dataSourceMode] = mode.name }
    }

    override val consentGiven: Flow<Boolean> = dataStore.data.map { it[Keys.consentGiven] ?: false }

    override suspend fun setConsentGiven(given: Boolean) {
        dataStore.edit { it[Keys.consentGiven] = given }
    }

    override val chargeUnitPriceWon: Flow<Double> = dataStore.data.map {
        it[Keys.chargeUnitPriceWon] ?: SettingsRepository.DEFAULT_CHARGE_UNIT_PRICE_WON
    }

    override suspend fun setChargeUnitPriceWon(won: Double) {
        dataStore.edit { it[Keys.chargeUnitPriceWon] = won }
    }

    // Names no longer in StationBrand are dropped rather than failing the whole set.
    override val preferredBrands: Flow<Set<StationBrand>> = dataStore.data.map { prefs ->
        prefs[Keys.preferredBrands].orEmpty().mapNotNull { runCatching { StationBrand.valueOf(it) }.getOrNull() }.toSet()
    }

    override suspend fun setPreferredBrands(brands: Set<StationBrand>) {
        dataStore.edit { it[Keys.preferredBrands] = brands.map { b -> b.name }.toSet() }
    }

    override val ecoGuideEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.ecoGuideEnabled] ?: false }

    override suspend fun setEcoGuideEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ecoGuideEnabled] = enabled }
    }

    override val ambientBaseColor: Flow<Int> = dataStore.data.map {
        it[Keys.ambientBaseColor] ?: SettingsRepository.DEFAULT_AMBIENT_COLOR
    }

    override suspend fun setAmbientBaseColor(rgb: Int) {
        dataStore.edit { it[Keys.ambientBaseColor] = rgb }
    }
}
