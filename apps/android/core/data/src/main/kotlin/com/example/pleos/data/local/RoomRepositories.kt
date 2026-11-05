package com.example.pleos.data.local

import com.example.pleos.data.local.dao.CareDao
import com.example.pleos.data.local.dao.DriveSessionDao
import com.example.pleos.data.local.dao.RecordDao
import com.example.pleos.data.local.dao.VehicleDao
import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.DriveSession
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.repository.CareRepository
import com.example.pleos.domain.repository.DriveSessionRepository
import com.example.pleos.domain.repository.RecordRepository
import com.example.pleos.domain.repository.VehicleRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomVehicleRepository @Inject constructor(private val dao: VehicleDao) : VehicleRepository {
    override val vehicle: Flow<Vehicle?> = dao.observeFirst().map { it?.toDomain() }
    override suspend fun current(): Vehicle? = dao.first()?.toDomain()
    override suspend fun save(vehicle: Vehicle) = dao.upsert(vehicle.toEntity())
}

@Singleton
internal class RoomRecordRepository @Inject constructor(private val dao: RecordDao) : RecordRepository {
    override fun observeConfirmed(vehicleId: String): Flow<List<Record>> =
        dao.observeByStatus(vehicleId, RecordStatus.CONFIRMED.name).map { rows -> rows.map { it.toDomain() } }

    override fun observeDrafts(vehicleId: String): Flow<List<Record>> =
        dao.observeByStatus(vehicleId, RecordStatus.DRAFT.name).map { rows -> rows.map { it.toDomain() } }

    override fun observe(id: String): Flow<Record?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun get(id: String): Record? = dao.get(id)?.toDomain()

    override suspend fun upsert(record: Record) = dao.upsert(record.toEntity())

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun latestDraft(vehicleId: String, type: RecordType, since: Instant): Record? =
        dao.latestDraft(vehicleId, type.name, since.toEpochMilli())?.toDomain()

    override suspend fun confirmedOfType(vehicleId: String, type: RecordType): List<Record> =
        dao.confirmedOfType(vehicleId, type.name).map { it.toDomain() }
}

@Singleton
internal class RoomDriveSessionRepository @Inject constructor(private val dao: DriveSessionDao) : DriveSessionRepository {
    override suspend fun upsert(session: DriveSession) = dao.upsert(session.toEntity())
    override suspend fun lastEnded(vehicleId: String): DriveSession? = dao.lastEnded(vehicleId)?.toDomain()
    override suspend fun open(vehicleId: String): List<DriveSession> = dao.open(vehicleId).map { it.toDomain() }
    override fun observeRecent(vehicleId: String, limit: Int): Flow<List<DriveSession>> =
        dao.observeRecent(vehicleId, limit).map { rows -> rows.map { it.toDomain() } }
}

@Singleton
internal class RoomCareRepository @Inject constructor(private val dao: CareDao) : CareRepository {
    override fun observeStates(vehicleId: String): Flow<List<CareItemState>> =
        dao.observe(vehicleId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun states(vehicleId: String): List<CareItemState> = dao.states(vehicleId).map { it.toDomain() }

    override suspend fun replaceAll(vehicleId: String, states: List<CareItemState>) {
        require(states.all { it.vehicleId == vehicleId }) { "States of another vehicle" }
        dao.replaceAll(vehicleId, states.map { it.toEntity() })
    }
}
