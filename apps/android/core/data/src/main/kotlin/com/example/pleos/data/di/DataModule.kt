package com.example.pleos.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.pleos.data.local.AppDatabase
import com.example.pleos.data.local.RoomCareRepository
import com.example.pleos.data.local.RoomDriveSessionRepository
import com.example.pleos.data.local.RoomRecordRepository
import com.example.pleos.data.local.RoomVehicleRepository
import com.example.pleos.data.local.dao.CareDao
import com.example.pleos.data.local.dao.DriveSessionDao
import com.example.pleos.data.local.dao.RecordDao
import com.example.pleos.data.local.dao.VehicleDao
import com.example.pleos.data.remote.ApiConfig
import com.example.pleos.data.settings.DataStoreSettingsRepository
import com.example.pleos.domain.repository.CareRepository
import com.example.pleos.domain.repository.DriveSessionRepository
import com.example.pleos.domain.repository.RecordRepository
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.repository.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindings {

    @Binds
    abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun vehicleRepository(impl: RoomVehicleRepository): VehicleRepository

    @Binds
    abstract fun recordRepository(impl: RoomRecordRepository): RecordRepository

    @Binds
    abstract fun driveSessionRepository(impl: RoomDriveSessionRepository): DriveSessionRepository

    @Binds
    abstract fun careRepository(impl: RoomCareRepository): CareRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @Singleton
    fun preferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun vehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun recordDao(db: AppDatabase): RecordDao = db.recordDao()

    @Provides
    fun driveSessionDao(db: AppDatabase): DriveSessionDao = db.driveSessionDao()

    @Provides
    fun careDao(db: AppDatabase): CareDao = db.careDao()

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.DEFAULT_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
