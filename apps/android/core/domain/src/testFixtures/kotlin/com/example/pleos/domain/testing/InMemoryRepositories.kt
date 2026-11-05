package com.example.pleos.domain.testing

import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.DriveSession
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.repository.CareRepository
import com.example.pleos.domain.repository.DriveSessionRepository
import com.example.pleos.domain.repository.RecordRepository
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.domain.repository.VehicleRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryVehicleRepository(initial: Vehicle? = null) : VehicleRepository {
    private val state = MutableStateFlow(initial)
    override val vehicle: Flow<Vehicle?> = state
    override suspend fun current(): Vehicle? = state.value
    override suspend fun save(vehicle: Vehicle) {
        state.value = vehicle
    }
}

class InMemoryRecordRepository : RecordRepository {
    val all = MutableStateFlow<Map<String, Record>>(emptyMap())

    private fun of(vehicleId: String, status: RecordStatus) =
        all.map { m -> m.values.filter { it.vehicleId == vehicleId && it.status == status }.sortedByDescending { it.occurredAt } }

    override fun observeConfirmed(vehicleId: String): Flow<List<Record>> = of(vehicleId, RecordStatus.CONFIRMED)
    override fun observeDrafts(vehicleId: String): Flow<List<Record>> = of(vehicleId, RecordStatus.DRAFT)
    override fun observe(id: String): Flow<Record?> = all.map { it[id] }
    override suspend fun get(id: String): Record? = all.value[id]
    override suspend fun upsert(record: Record) = all.update { it + (record.id to record) }
    override suspend fun delete(id: String) = all.update { it - id }

    override suspend fun latestDraft(vehicleId: String, type: RecordType, since: Instant): Record? =
        all.value.values
            .filter { it.vehicleId == vehicleId && it.type == type && it.status == RecordStatus.DRAFT && it.occurredAt >= since }
            .maxByOrNull { it.occurredAt }

    override suspend fun confirmedOfType(vehicleId: String, type: RecordType): List<Record> =
        all.value.values.filter { it.vehicleId == vehicleId && it.type == type && it.status == RecordStatus.CONFIRMED }
}

class InMemoryDriveSessionRepository : DriveSessionRepository {
    val all = MutableStateFlow<Map<String, DriveSession>>(emptyMap())

    override suspend fun upsert(session: DriveSession) = all.update { it + (session.id to session) }

    override suspend fun lastEnded(vehicleId: String): DriveSession? =
        all.value.values.filter { it.vehicleId == vehicleId && it.end != null }.maxByOrNull { it.end!!.capturedAt }

    override suspend fun open(vehicleId: String): List<DriveSession> =
        all.value.values.filter { it.vehicleId == vehicleId && it.end == null }

    override fun observeRecent(vehicleId: String, limit: Int): Flow<List<DriveSession>> =
        all.map { m -> m.values.filter { it.vehicleId == vehicleId }.sortedByDescending { it.start.capturedAt }.take(limit) }
}

class InMemoryCareRepository : CareRepository {
    val all = MutableStateFlow<List<CareItemState>>(emptyList())

    override fun observeStates(vehicleId: String): Flow<List<CareItemState>> = all.map { l -> l.filter { it.vehicleId == vehicleId } }
    override suspend fun states(vehicleId: String): List<CareItemState> = all.value.filter { it.vehicleId == vehicleId }
    override suspend fun replaceAll(vehicleId: String, states: List<CareItemState>) =
        all.update { l -> l.filter { it.vehicleId != vehicleId } + states }
}

class InMemorySettingsRepository(
    mode: DataSourceMode = DataSourceMode.DEMO,
    price: Double = SettingsRepository.DEFAULT_CHARGE_UNIT_PRICE_WON,
) : SettingsRepository {
    private val modeState = MutableStateFlow(mode)
    private val consent = MutableStateFlow(false)
    private val priceState = MutableStateFlow(price)

    override val dataSourceMode: Flow<DataSourceMode> = modeState
    override suspend fun setDataSourceMode(mode: DataSourceMode) { modeState.value = mode }
    override val consentGiven: Flow<Boolean> = consent
    override suspend fun setConsentGiven(given: Boolean) { consent.value = given }
    override val chargeUnitPriceWon: Flow<Double> = priceState
    override suspend fun setChargeUnitPriceWon(won: Double) { priceState.value = won }
}
