package com.example.pleos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.pleos.data.local.entity.CareItemStateEntity
import com.example.pleos.data.local.entity.DriveSessionEntity
import com.example.pleos.data.local.entity.RecordEntity
import com.example.pleos.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface VehicleDao {
    @Query("SELECT * FROM vehicle ORDER BY baselineAt LIMIT 1")
    fun observeFirst(): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicle ORDER BY baselineAt LIMIT 1")
    suspend fun first(): VehicleEntity?

    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)
}

@Dao
internal interface RecordDao {
    @Query("SELECT * FROM record WHERE vehicleId = :vehicleId AND status = :status ORDER BY occurredAt DESC")
    fun observeByStatus(vehicleId: String, status: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM record WHERE id = :id")
    fun observe(id: String): Flow<RecordEntity?>

    @Query("SELECT * FROM record WHERE id = :id")
    suspend fun get(id: String): RecordEntity?

    @Upsert
    suspend fun upsert(record: RecordEntity)

    @Query("DELETE FROM record WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT * FROM record WHERE vehicleId = :vehicleId AND type = :type AND status = 'DRAFT' " +
            "AND occurredAt >= :since ORDER BY occurredAt DESC LIMIT 1",
    )
    suspend fun latestDraft(vehicleId: String, type: String, since: Long): RecordEntity?

    @Query("SELECT * FROM record WHERE vehicleId = :vehicleId AND type = :type AND status = 'CONFIRMED'")
    suspend fun confirmedOfType(vehicleId: String, type: String): List<RecordEntity>
}

@Dao
internal interface DriveSessionDao {
    @Upsert
    suspend fun upsert(session: DriveSessionEntity)

    @Query("SELECT * FROM drive_session WHERE vehicleId = :vehicleId AND endCapturedAt IS NOT NULL ORDER BY endCapturedAt DESC LIMIT 1")
    suspend fun lastEnded(vehicleId: String): DriveSessionEntity?

    @Query("SELECT * FROM drive_session WHERE vehicleId = :vehicleId AND endCapturedAt IS NULL")
    suspend fun open(vehicleId: String): List<DriveSessionEntity>

    @Query("SELECT * FROM drive_session WHERE vehicleId = :vehicleId ORDER BY startCapturedAt DESC LIMIT :limit")
    fun observeRecent(vehicleId: String, limit: Int): Flow<List<DriveSessionEntity>>
}

@Dao
internal interface CareDao {
    @Query("SELECT * FROM care_item_state WHERE vehicleId = :vehicleId")
    fun observe(vehicleId: String): Flow<List<CareItemStateEntity>>

    @Query("SELECT * FROM care_item_state WHERE vehicleId = :vehicleId")
    suspend fun states(vehicleId: String): List<CareItemStateEntity>

    @Query("DELETE FROM care_item_state WHERE vehicleId = :vehicleId")
    suspend fun deleteAll(vehicleId: String)

    @Upsert
    suspend fun upsertAll(states: List<CareItemStateEntity>)

    @Transaction
    suspend fun replaceAll(vehicleId: String, states: List<CareItemStateEntity>) {
        deleteAll(vehicleId)
        upsertAll(states)
    }
}
