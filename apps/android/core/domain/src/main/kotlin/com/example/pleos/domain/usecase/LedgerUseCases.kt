package com.example.pleos.domain.usecase

import com.example.pleos.domain.care.CareProgressCalculator
import com.example.pleos.domain.care.CareStateRebuilder
import com.example.pleos.domain.gateway.IdGenerator
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.ledger.RecordFactory
import com.example.pleos.domain.model.CareCatalog
import com.example.pleos.domain.model.CareItemDef
import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.CareProgress
import com.example.pleos.domain.model.CareStatus
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.ExpenseCategory
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleStamp
import com.example.pleos.domain.repository.CareRepository
import com.example.pleos.domain.repository.DriveSessionRepository
import com.example.pleos.domain.repository.RecordRepository
import com.example.pleos.domain.repository.VehicleRepository
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Builds a [VehicleStamp] from the live vehicle (L1-1), falling back to the last known odometer. */
class StampVehicleUseCase @Inject constructor(
    private val gateway: VehicleGateway,
    private val sessions: DriveSessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(vehicle: Vehicle): VehicleStamp {
        val live = gateway.snapshot.first().cumulativeDistanceKm
        val source = if (gateway.activeSource.first() == DataSourceMode.REAL) StampSource.VEHICLE else StampSource.DEMO
        val fallback = sessions.lastEnded(vehicle.id)?.end?.odometerKm ?: vehicle.baselineOdometerKm
        return RecordFactory.stamp(live, vehicle.vinHash, Instant.now(clock), source, fallback)
    }
}

/** One care item with its catalog row, stored state and computed progress. */
data class CareItemView(
    val def: CareItemDef,
    val state: CareItemState?,
    val progress: CareProgress,
)

/**
 * Care items for the current vehicle, most urgent first. Recomputed whenever the live odometer or
 * a state changes. The odometer always comes from the vehicle, never from the last record (L3-1).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCareItemsUseCase @Inject constructor(
    private val vehicles: VehicleRepository,
    private val care: CareRepository,
    private val gateway: VehicleGateway,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<CareItemView>> = vehicles.vehicle.filterNotNull().flatMapLatest { vehicle ->
        val odometer = gateway.snapshot.map { it.cumulativeDistanceKm }.distinctUntilChanged()
        combine(care.observeStates(vehicle.id), odometer) { states, km ->
            val byId = states.associateBy { it.itemId }
            CareCatalog.forEnergyType(vehicle.energyType)
                .map { def ->
                    val state = byId[def.id]
                    CareItemView(def, state, CareProgressCalculator.progress(def, state, km, Instant.now(clock), clock.zone))
                }
                .sortedWith(urgency)
        }
    }

    private val urgency: Comparator<CareItemView> = compareBy<CareItemView> {
        when (it.progress.status) {
            CareStatus.OVERDUE -> 0
            CareStatus.DUE_SOON -> 1
            CareStatus.OK -> 2
            CareStatus.UNKNOWN -> 3
        }
    }.thenByDescending { it.progress.ratio ?: 0.0 }
}

/**
 * Saves a confirmed maintenance record and rebuilds the care states (L1-2, L3-2).
 * [serviceOdometerKm] and [occurredAt] are for a past service entered later; leave them null for now.
 */
class RecordMaintenanceUseCase @Inject constructor(
    private val vehicles: VehicleRepository,
    private val records: RecordRepository,
    private val care: CareRepository,
    private val stampVehicle: StampVehicleUseCase,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        itemIds: List<String>,
        amountWon: Long? = null,
        shopName: String? = null,
        occurredAt: Instant? = null,
        serviceOdometerKm: Double? = null,
        origin: RecordOrigin = RecordOrigin.MANUAL,
    ): Record {
        require(itemIds.isNotEmpty()) { "A maintenance record needs at least one item" }
        val vehicle = checkNotNull(vehicles.current()) { "No vehicle registered yet" }
        val stamp = stampVehicle(vehicle)
        val record = Record(
            id = ids.newId(),
            vehicleId = vehicle.id,
            type = RecordType.MAINTENANCE,
            occurredAt = occurredAt ?: Instant.now(clock),
            amountWon = amountWon,
            stamp = stamp,
            origin = origin,
            status = RecordStatus.CONFIRMED,
            detail = RecordDetail.Maintenance(itemIds, shopName, serviceOdometerKm),
        ).let { it.copy(integrityHash = RecordFactory.integrityHash(it)) }
        records.upsert(record)
        rebuildCare(vehicle.id, records, care)
        return record
    }
}

/** Deletes a record; deleting a maintenance record rolls the care states back (L3-3). */
class DeleteRecordUseCase @Inject constructor(
    private val records: RecordRepository,
    private val care: CareRepository,
) {
    suspend operator fun invoke(id: String) {
        val record = records.get(id) ?: return
        records.delete(id)
        if (record.type == RecordType.MAINTENANCE) rebuildCare(record.vehicleId, records, care)
    }
}

/** The newest detected draft waiting for the user, or null. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObservePendingDraftUseCase @Inject constructor(
    private val vehicles: VehicleRepository,
    private val records: RecordRepository,
) {
    operator fun invoke(): Flow<Record?> = vehicles.vehicle.flatMapLatest { vehicle ->
        if (vehicle == null) flowOf(null) else records.observeDrafts(vehicle.id).map { it.firstOrNull() }
    }
}

class ConfirmDraftUseCase @Inject constructor(
    private val records: RecordRepository,
) {
    /** Returns false when the draft is gone or already handled. */
    suspend operator fun invoke(id: String, amountWon: Long?): Boolean {
        val draft = records.get(id)?.takeIf { it.status == RecordStatus.DRAFT } ?: return false
        records.upsert(RecordFactory.confirm(draft, amountWon))
        return true
    }
}

class DismissDraftUseCase @Inject constructor(
    private val records: RecordRepository,
) {
    suspend operator fun invoke(id: String) {
        val draft = records.get(id)?.takeIf { it.status == RecordStatus.DRAFT } ?: return
        records.upsert(draft.copy(status = RecordStatus.DISMISSED))
    }
}

internal suspend fun rebuildCare(vehicleId: String, records: RecordRepository, care: CareRepository) {
    val maintenance = records.confirmedOfType(vehicleId, RecordType.MAINTENANCE)
    care.replaceAll(vehicleId, CareStateRebuilder.rebuild(vehicleId, maintenance, care.states(vehicleId)))
}

/** Drafts waiting for the user and confirmed records, newest first: the ledger list. */
data class LedgerEntries(val drafts: List<Record> = emptyList(), val confirmed: List<Record> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveLedgerUseCase @Inject constructor(
    private val vehicles: VehicleRepository,
    private val records: RecordRepository,
) {
    operator fun invoke(): Flow<LedgerEntries> = vehicles.vehicle.flatMapLatest { vehicle ->
        if (vehicle == null) {
            flowOf(LedgerEntries())
        } else {
            combine(records.observeDrafts(vehicle.id), records.observeConfirmed(vehicle.id)) { d, c -> LedgerEntries(d, c) }
        }
    }
}

/** Confirmed spending and energy in the current calendar month. Drafts are not counted (Record status DRAFT). */
data class MonthSummary(
    val month: Int,
    val spentWon: Long,
    val energyRecords: Int,
    /** kWh for an EV, L for a fuel car; records whose amount is unknown are skipped. */
    val energyAmount: Double,
)

fun monthSummary(confirmed: List<Record>, now: Instant, zone: ZoneId): MonthSummary {
    val today = now.atZone(zone).toLocalDate()
    val thisMonth = confirmed.filter {
        val day = it.occurredAt.atZone(zone).toLocalDate()
        day.year == today.year && day.month == today.month
    }
    val energy = thisMonth.mapNotNull { it.detail as? RecordDetail.Energy }
    return MonthSummary(
        month = today.monthValue,
        spentWon = thisMonth.sumOf { it.amountWon ?: 0L },
        energyRecords = energy.size,
        energyAmount = energy.sumOf { it.amount ?: 0.0 },
    )
}

/** Money of one kind in a month: energy, maintenance, or one expense category. */
data class SpendGroup(val type: RecordType, val category: ExpenseCategory?, val count: Int, val won: Long)

/** One row of the monthly ledger table. Confirmed records only. */
data class MonthLedger(
    val year: Int,
    val month: Int,
    /** Driven in the month from the odometer stamps; null with fewer than two readings. */
    val distanceKm: Double?,
    /** kWh for an EV, L for a fuel car. */
    val energyAmount: Double,
    val spentWon: Long,
    /** Largest first. */
    val groups: List<SpendGroup>,
)

/**
 * The last [count] months up to the current one, newest first. A month's distance is its last
 * odometer reading minus the last reading before it (or its own first, for the oldest data).
 */
fun monthLedgers(confirmed: List<Record>, now: Instant, zone: ZoneId, count: Int = 6): List<MonthLedger> {
    val current = YearMonth.from(now.atZone(zone))
    val byMonth = confirmed.groupBy { YearMonth.from(it.occurredAt.atZone(zone)) }
    return (0 until count).map { back ->
        val ym = current.minusMonths(back.toLong())
        val records = byMonth[ym].orEmpty()
        val start = ym.atDay(1).atStartOfDay(zone).toInstant()
        val before = confirmed.filter { it.occurredAt < start }.maxOfOrNull { it.stamp.odometerKm }
        val readings = records.map { it.stamp.odometerKm }
        val distance = readings.maxOrNull()?.let { last ->
            val from = before ?: readings.min().takeIf { readings.size > 1 }
            from?.let { (last - it).coerceAtLeast(0.0) }
        }
        val groups = records
            .groupBy { it.type to (it.detail as? RecordDetail.Expense)?.category }
            .map { (key, list) -> SpendGroup(key.first, key.second, list.size, list.sumOf { it.amountWon ?: 0L }) }
            .sortedByDescending { it.won }
        MonthLedger(
            year = ym.year,
            month = ym.monthValue,
            distanceKm = distance,
            energyAmount = records.mapNotNull { (it.detail as? RecordDetail.Energy)?.amount }.sum(),
            spentWon = records.sumOf { it.amountWon ?: 0L },
            groups = groups,
        )
    }
}
