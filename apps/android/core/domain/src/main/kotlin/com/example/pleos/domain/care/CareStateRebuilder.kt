package com.example.pleos.domain.care

import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordStatus

/**
 * FEATURE_LOGIC L3-2 and L3-3. Care item states are derived from the confirmed maintenance
 * records, so saving, back-filling and deleting a record all come down to rebuilding.
 *
 * For each item the latest service wins, by service date and then odometer. A past record entered
 * later therefore never overwrites a newer one, and deleting a record falls back to the next latest.
 * User-set custom intervals are kept even when an item has no record left.
 */
object CareStateRebuilder {

    fun rebuild(vehicleId: String, records: List<Record>, previous: List<CareItemState>): List<CareItemState> {
        val services = records
            .filter { it.vehicleId == vehicleId && it.status == RecordStatus.CONFIRMED }
            .mapNotNull { record -> (record.detail as? RecordDetail.Maintenance)?.let { record to it } }

        val latestByItem = services
            .flatMap { (record, detail) -> detail.itemIds.map { itemId -> Triple(itemId, record, detail) } }
            .groupBy { it.first }
            .mapValues { (_, entries) ->
                entries.maxWith(compareBy({ it.second.occurredAt }, { serviceOdometer(it.second, it.third) }))
            }

        val customs = previous.filter { it.vehicleId == vehicleId }.associateBy { it.itemId }
        val itemIds = (latestByItem.keys + customs.filterValues { it.customIntervalKm != null || it.customIntervalMonths != null }.keys)

        return itemIds.sorted().map { itemId ->
            val custom = customs[itemId]
            val latest = latestByItem[itemId]
            CareItemState(
                vehicleId = vehicleId,
                itemId = itemId,
                lastServiceOdometerKm = latest?.let { serviceOdometer(it.second, it.third) },
                lastServiceAt = latest?.second?.occurredAt,
                customIntervalKm = custom?.customIntervalKm,
                customIntervalMonths = custom?.customIntervalMonths,
                lastRecordId = latest?.second?.id,
            )
        }
    }

    private fun serviceOdometer(record: Record, detail: RecordDetail.Maintenance): Double =
        detail.serviceOdometerKm ?: record.stamp.odometerKm
}
