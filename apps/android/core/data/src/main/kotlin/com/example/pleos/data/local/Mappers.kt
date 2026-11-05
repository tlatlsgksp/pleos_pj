package com.example.pleos.data.local

import com.example.pleos.data.local.entity.CareItemStateEntity
import com.example.pleos.data.local.entity.DriveSessionEntity
import com.example.pleos.data.local.entity.RecordEntity
import com.example.pleos.data.local.entity.VehicleEntity
import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.DriveSession
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.ExpenseCategory
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.SessionKind
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleStamp
import java.time.Instant

private fun Instant.millis(): Long = toEpochMilli()
private fun Long.instant(): Instant = Instant.ofEpochMilli(this)

internal fun VehicleEntity.toDomain() = Vehicle(
    id = id,
    energyType = EnergyType.valueOf(energyType),
    vinHash = vinHash,
    tankCapacityL = tankCapacityL,
    batteryCapacityKwh = batteryCapacityKwh,
    baselineOdometerKm = baselineOdometerKm,
    baselineAt = baselineAt.instant(),
)

internal fun Vehicle.toEntity() = VehicleEntity(
    id = id,
    energyType = energyType.name,
    vinHash = vinHash,
    tankCapacityL = tankCapacityL,
    batteryCapacityKwh = batteryCapacityKwh,
    baselineOdometerKm = baselineOdometerKm,
    baselineAt = baselineAt.millis(),
)

internal fun RecordEntity.toDomain(): Record {
    val type = RecordType.valueOf(type)
    val detail = when (type) {
        RecordType.ENERGY -> RecordDetail.Energy(
            kind = EnergyType.valueOf(checkNotNull(energyKind) { "Energy record $id without kind" }),
            amount = energyAmount,
            unitPriceWon = unitPriceWon,
            placeName = placeName,
            levelBefore = levelBefore,
            levelAfter = levelAfter,
        )
        RecordType.MAINTENANCE -> RecordDetail.Maintenance(
            itemIds = maintenanceItemIds?.split(',')?.filter { it.isNotEmpty() }.orEmpty(),
            shopName = shopName,
            serviceOdometerKm = serviceOdometerKm,
        )
        RecordType.EXPENSE -> RecordDetail.Expense(
            category = expenseCategory?.let(ExpenseCategory::valueOf) ?: ExpenseCategory.OTHER,
            customLabel = expenseCustomLabel,
        )
        RecordType.ACCIDENT -> RecordDetail.Accident(
            speedKmh = accidentSpeedKmh,
            latitude = accidentLatitude,
            longitude = accidentLongitude,
            driveSessionId = accidentDriveSessionId,
        )
    }
    return Record(
        id = id,
        vehicleId = vehicleId,
        type = type,
        occurredAt = occurredAt.instant(),
        amountWon = amountWon,
        stamp = VehicleStamp(stampOdometerKm, stampVinHash, stampCapturedAt.instant(), StampSource.valueOf(stampSource)),
        origin = RecordOrigin.valueOf(origin),
        status = RecordStatus.valueOf(status),
        detail = detail,
        memo = memo,
        rentalPeriodId = rentalPeriodId,
        editedAt = editedAt?.instant(),
        integrityHash = integrityHash,
    )
}

internal fun Record.toEntity(): RecordEntity {
    val base = RecordEntity(
        id = id,
        vehicleId = vehicleId,
        type = type.name,
        occurredAt = occurredAt.millis(),
        amountWon = amountWon,
        stampOdometerKm = stamp.odometerKm,
        stampVinHash = stamp.vinHash,
        stampCapturedAt = stamp.capturedAt.millis(),
        stampSource = stamp.source.name,
        origin = origin.name,
        status = status.name,
        memo = memo,
        rentalPeriodId = rentalPeriodId,
        editedAt = editedAt?.millis(),
        integrityHash = integrityHash,
    )
    return when (val d = detail) {
        is RecordDetail.Energy -> base.copy(
            energyKind = d.kind.name,
            energyAmount = d.amount,
            unitPriceWon = d.unitPriceWon,
            placeName = d.placeName,
            levelBefore = d.levelBefore,
            levelAfter = d.levelAfter,
        )
        is RecordDetail.Maintenance -> {
            require(d.itemIds.none { ',' in it }) { "Care item ids must not contain ','" }
            base.copy(maintenanceItemIds = d.itemIds.joinToString(","), shopName = d.shopName, serviceOdometerKm = d.serviceOdometerKm)
        }
        is RecordDetail.Expense -> base.copy(expenseCategory = d.category.name, expenseCustomLabel = d.customLabel)
        is RecordDetail.Accident -> base.copy(
            accidentSpeedKmh = d.speedKmh,
            accidentLatitude = d.latitude,
            accidentLongitude = d.longitude,
            accidentDriveSessionId = d.driveSessionId,
        )
    }
}

internal fun DriveSessionEntity.toDomain() = DriveSession(
    id = id,
    vehicleId = vehicleId,
    start = VehicleStamp(startOdometerKm, startVinHash, startCapturedAt.instant(), StampSource.valueOf(startSource)),
    end = if (endOdometerKm != null && endCapturedAt != null && endSource != null) {
        VehicleStamp(endOdometerKm, endVinHash, endCapturedAt.instant(), StampSource.valueOf(endSource))
    } else {
        null
    },
    distanceKm = distanceKm,
    energyUsed = energyUsed,
    idleSeconds = idleSeconds,
    idleEnergyUsed = idleEnergyUsed,
    tollWon = tollWon,
    destinationName = destinationName,
    kind = SessionKind.valueOf(kind),
)

internal fun DriveSession.toEntity() = DriveSessionEntity(
    id = id,
    vehicleId = vehicleId,
    kind = kind.name,
    startOdometerKm = start.odometerKm,
    startVinHash = start.vinHash,
    startCapturedAt = start.capturedAt.millis(),
    startSource = start.source.name,
    endOdometerKm = end?.odometerKm,
    endVinHash = end?.vinHash,
    endCapturedAt = end?.capturedAt?.millis(),
    endSource = end?.source?.name,
    distanceKm = distanceKm,
    energyUsed = energyUsed,
    idleSeconds = idleSeconds,
    idleEnergyUsed = idleEnergyUsed,
    tollWon = tollWon,
    destinationName = destinationName,
)

internal fun CareItemStateEntity.toDomain() = CareItemState(
    vehicleId = vehicleId,
    itemId = itemId,
    lastServiceOdometerKm = lastServiceOdometerKm,
    lastServiceAt = lastServiceAt?.instant(),
    customIntervalKm = customIntervalKm,
    customIntervalMonths = customIntervalMonths,
    lastRecordId = lastRecordId,
)

internal fun CareItemState.toEntity() = CareItemStateEntity(
    vehicleId = vehicleId,
    itemId = itemId,
    lastServiceOdometerKm = lastServiceOdometerKm,
    lastServiceAt = lastServiceAt?.millis(),
    customIntervalKm = customIntervalKm,
    customIntervalMonths = customIntervalMonths,
    lastRecordId = lastRecordId,
)
