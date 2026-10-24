package com.example.pleos.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.pleos.domain.model.DataSourceMode
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
    }

    // DEMO until a real gateway exists; flip the default to REAL once the Pleos wrappers are bound.
    override val dataSourceMode: Flow<DataSourceMode> = dataStore.data.map { prefs ->
        prefs[Keys.dataSourceMode]?.let { runCatching { DataSourceMode.valueOf(it) }.getOrNull() }
            ?: DataSourceMode.DEMO
    }

    override suspend fun setDataSourceMode(mode: DataSourceMode) {
        dataStore.edit { it[Keys.dataSourceMode] = mode.name }
    }

    override val consentGiven: Flow<Boolean> = dataStore.data.map { it[Keys.consentGiven] ?: false }

    override suspend fun setConsentGiven(given: Boolean) {
        dataStore.edit { it[Keys.consentGiven] = given }
    }
}
