package com.example.pleos.domain.repository

import com.example.pleos.domain.model.DataSourceMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val dataSourceMode: Flow<DataSourceMode>

    suspend fun setDataSourceMode(mode: DataSourceMode)

    /** Whether the user agreed to send pseudonymized records to the server. Nothing is uploaded before this. */
    val consentGiven: Flow<Boolean>

    suspend fun setConsentGiven(given: Boolean)
}
