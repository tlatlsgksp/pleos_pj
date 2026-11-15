package com.example.pleos.domain.model

import java.time.Instant

enum class RecordType { ENERGY, MAINTENANCE, EXPENSE, ACCIDENT }

enum class RecordOrigin { MANUAL, DETECTED, VOICE, NAV_VISIT }

enum class RecordStatus {
    /** Detected from vehicle signals and waiting for the user. Not counted in statistics. */
    DRAFT,
    CONFIRMED,
    DISMISSED,
}

enum class ExpenseCategory { CAR_WASH, PARKING, TOLL, INSURANCE, TAX, OTHER, CUSTOM }

sealed interface RecordDetail {
    /** A refuel or a charge. [amount] is L for FUEL and kWh for EV; null when it could not be derived. */
    data class Energy(
        val kind: EnergyType,
        val amount: Double?,
        val unitPriceWon: Double? = null,
        val placeName: String? = null,
        /** Fuel % or battery % before and after. */
        val levelBefore: Double? = null,
        val levelAfter: Double? = null,
    ) : RecordDetail

    data class Maintenance(
        /** [CareItemDef.id]s serviced in this visit. */
        val itemIds: List<String>,
        val shopName: String? = null,
        /**
         * Odometer the user says the service happened at, for a past service entered later.
         * null means the service happened when the record was made, so the stamp's odometer applies.
         * The stamp itself always stays the vehicle reading at entry time.
         */
        val serviceOdometerKm: Double? = null,
    ) : RecordDetail

    data class Expense(
        val category: ExpenseCategory,
        /** Label when [category] is [ExpenseCategory.CUSTOM]. */
        val customLabel: String? = null,
    ) : RecordDetail

    data class Accident(
        val speedKmh: Double? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val driveSessionId: String? = null,
    ) : RecordDetail
}

/**
 * One event that changed money or the car's condition: refuel, charge, maintenance, expense or
 * accident. A single entity so statistics and the maintenance history come from one query.
 * Replaces the old fuel-only RefuelDraft: a detected charge is a Record with status DRAFT.
 */
data class Record(
    val id: String,
    val vehicleId: String,
    val type: RecordType,
    val occurredAt: Instant,
    /** Amount paid. null for accidents or when not entered yet. */
    val amountWon: Long? = null,
    val stamp: VehicleStamp,
    val origin: RecordOrigin,
    val status: RecordStatus,
    val detail: RecordDetail,
    val memo: String? = null,
    val rentalPeriodId: String? = null,
    /** Set when the user changes a saved record. The history shows such records as edited. */
    val editedAt: Instant? = null,
    /** Hash over the stamp and the core fields, made at save time. */
    val integrityHash: String? = null,
)
