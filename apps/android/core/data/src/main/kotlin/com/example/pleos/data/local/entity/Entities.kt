package com.example.pleos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Room rows. Times are epoch milliseconds, enums are their names. Each row maps 1:1 to a domain
// model in Mappers.kt; the domain never sees these classes. Every record and session row carries
// the vehicle stamp columns (odometer, VIN hash, time, source) from schema version 1.

/** Not yet synced to the backend; the sync worker flips it to SYNCED. */
internal const val SYNC_PENDING = "PENDING"

@Entity(tableName = "vehicle")
internal data class VehicleEntity(
    @PrimaryKey val id: String,
    val energyType: String,
    val vinHash: String?,
    val tankCapacityL: Double?,
    val batteryCapacityKwh: Double?,
    val baselineOdometerKm: Double,
    val baselineAt: Long,
)

@Entity(
    tableName = "record",
    indices = [Index("vehicleId", "status", "occurredAt"), Index("vehicleId", "type")],
)
internal data class RecordEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val type: String,
    val occurredAt: Long,
    val amountWon: Long?,
    val stampOdometerKm: Double,
    val stampVinHash: String?,
    val stampCapturedAt: Long,
    val stampSource: String,
    val origin: String,
    val status: String,
    val memo: String?,
    val rentalPeriodId: String?,
    val editedAt: Long?,
    val integrityHash: String?,
    val syncState: String = SYNC_PENDING,

    // RecordDetail.Energy
    val energyKind: String? = null,
    val energyAmount: Double? = null,
    val unitPriceWon: Double? = null,
    val placeName: String? = null,
    val levelBefore: Double? = null,
    val levelAfter: Double? = null,

    // RecordDetail.Maintenance. Item ids joined with ','.
    val maintenanceItemIds: String? = null,
    val shopName: String? = null,
    val serviceOdometerKm: Double? = null,

    // RecordDetail.Expense
    val expenseCategory: String? = null,
    val expenseCustomLabel: String? = null,

    // RecordDetail.Accident
    val accidentSpeedKmh: Double? = null,
    val accidentLatitude: Double? = null,
    val accidentLongitude: Double? = null,
    val accidentDriveSessionId: String? = null,
)

@Entity(tableName = "drive_session", indices = [Index("vehicleId", "endCapturedAt")])
internal data class DriveSessionEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val kind: String,
    val startOdometerKm: Double,
    val startVinHash: String?,
    val startCapturedAt: Long,
    val startSource: String,
    // All null while the session is open.
    val endOdometerKm: Double?,
    val endVinHash: String?,
    val endCapturedAt: Long?,
    val endSource: String?,
    val distanceKm: Double,
    val energyUsed: Double?,
    val idleSeconds: Long,
    val idleEnergyUsed: Double?,
    val tollWon: Long?,
    val destinationName: String?,
    val syncState: String = SYNC_PENDING,
)

@Entity(tableName = "drive_sample", primaryKeys = ["sessionId", "at"])
internal data class DriveSampleEntity(
    val sessionId: String,
    val at: Long,
    val speedKmh: Double?,
    val gear: String?,
    val energyRemaining: Double?,
    val hvacOn: Boolean?,
    val brakePedalPressed: Boolean?,
    val latitude: Double?,
    val longitude: Double?,
)

@Entity(tableName = "care_item_state", primaryKeys = ["vehicleId", "itemId"])
internal data class CareItemStateEntity(
    val vehicleId: String,
    val itemId: String,
    val lastServiceOdometerKm: Double?,
    val lastServiceAt: Long?,
    val customIntervalKm: Int?,
    val customIntervalMonths: Int?,
    val lastRecordId: String?,
)

@Entity(tableName = "rental_period", indices = [Index("vehicleId")])
internal data class RentalPeriodEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val start: Long,
    val end: Long?,
    val startOdometerKm: Double,
    val startVinHash: String?,
    val startCapturedAt: Long,
    val startSource: String,
    val endOdometerKm: Double?,
    val endVinHash: String?,
    val endCapturedAt: Long?,
    val endSource: String?,
)

@Entity(tableName = "pending_visit", indices = [Index("vehicleId")])
internal data class PendingVisitEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val shopName: String,
    val arrivedAt: Long,
    val arrivedOdometerKm: Double,
    val arrivedVinHash: String?,
    val arrivedCapturedAt: Long,
    val arrivedSource: String,
)
