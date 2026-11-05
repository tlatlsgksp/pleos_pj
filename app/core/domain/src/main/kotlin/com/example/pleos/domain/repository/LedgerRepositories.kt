package com.example.pleos.domain.repository

import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.DriveSession
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.Vehicle
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** The car the ledger belongs to. One vehicle for now. */
interface VehicleRepository {
    val vehicle: Flow<Vehicle?>

    suspend fun current(): Vehicle?

    suspend fun save(vehicle: Vehicle)
}

interface RecordRepository {
    /** Newest first. */
    fun observeConfirmed(vehicleId: String): Flow<List<Record>>

    /** Newest first. */
    fun observeDrafts(vehicleId: String): Flow<List<Record>>

    fun observe(id: String): Flow<Record?>

    suspend fun get(id: String): Record?

    suspend fun upsert(record: Record)

    suspend fun delete(id: String)

    /** Newest DRAFT of [type] that occurred at or after [since], for de-duplicating detections (L1-1). */
    suspend fun latestDraft(vehicleId: String, type: RecordType, since: Instant): Record?

    suspend fun confirmedOfType(vehicleId: String, type: RecordType): List<Record>
}

interface DriveSessionRepository {
    /** Inserts or replaces by id. An open session is saved with `end == null` and replaced when it ends. */
    suspend fun upsert(session: DriveSession)

    /** Most recent session that has ended. */
    suspend fun lastEnded(vehicleId: String): DriveSession?

    /** Sessions still marked open, left behind when the app died mid-drive. */
    suspend fun open(vehicleId: String): List<DriveSession>

    /** Newest first. */
    fun observeRecent(vehicleId: String, limit: Int): Flow<List<DriveSession>>
}

interface CareRepository {
    fun observeStates(vehicleId: String): Flow<List<CareItemState>>

    suspend fun states(vehicleId: String): List<CareItemState>

    /** Replaces every state of the vehicle; states are derived from records (L3-3). */
    suspend fun replaceAll(vehicleId: String, states: List<CareItemState>)
}
