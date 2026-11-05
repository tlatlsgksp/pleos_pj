package com.example.pleos.domain.model

import java.time.Instant
import java.time.LocalDate

/** One row of the built-in maintenance table (see [CareCatalog]). */
data class CareItemDef(
    val id: String,
    val name: String,
    /** Engine oil = FUEL only, reducer oil = EV only, cabin filter = both. */
    val energyTypes: Set<EnergyType>,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    /** Where the default interval comes from. */
    val source: String,
)

/**
 * Per-vehicle state of one care item. A derived value: it can always be rebuilt from the
 * confirmed maintenance records (FEATURE_LOGIC L3-3).
 */
data class CareItemState(
    val vehicleId: String,
    val itemId: String,
    val lastServiceOdometerKm: Double? = null,
    val lastServiceAt: Instant? = null,
    /** User overrides of the catalog interval. */
    val customIntervalKm: Int? = null,
    val customIntervalMonths: Int? = null,
    /** The maintenance record that produced this state. */
    val lastRecordId: String? = null,
)

enum class CareStatus {
    /** Progress below 0.8. */
    OK,

    /** 0.8 <= progress < 1.0. */
    DUE_SOON,

    /** Progress >= 1.0. */
    OVERDUE,

    /** No last-service information, so no progress. The UI asks the user for it. */
    UNKNOWN,
}

/** Result of the progress rule (FEATURE_LOGIC L3-1) for one item. 1.0 means the interval is reached. */
data class CareProgress(
    val itemId: String,
    val status: CareStatus,
    /** max(distanceRatio, monthsRatio); null when neither can be computed. */
    val ratio: Double?,
    val distanceRatio: Double?,
    val monthsRatio: Double?,
    val remainingKm: Double?,
    /** Whole calendar months since the last service. */
    val elapsedMonths: Int?,
    /** Last service date plus the interval in calendar months. */
    val dueDate: LocalDate?,
)
