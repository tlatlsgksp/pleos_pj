package com.example.pleos.domain.ledger

import com.example.pleos.domain.drive.EnergyEvent
import com.example.pleos.domain.model.Record
import com.example.pleos.domain.model.RecordDetail
import com.example.pleos.domain.model.RecordOrigin
import com.example.pleos.domain.model.RecordStatus
import com.example.pleos.domain.model.RecordType
import com.example.pleos.domain.model.StampSource
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleStamp
import java.security.MessageDigest
import java.time.Instant
import kotlin.math.roundToLong

/** FEATURE_LOGIC L1-1: how records get their stamp, their draft form and their integrity hash. */
object RecordFactory {

    /**
     * Builds the stamp from the live odometer. When the vehicle gives none, falls back to the
     * last known odometer (last session end or the baseline) and marks the stamp MANUAL.
     */
    fun stamp(
        liveOdometerKm: Double?,
        vinHash: String?,
        now: Instant,
        liveSource: StampSource,
        fallbackOdometerKm: Double,
    ): VehicleStamp = if (liveOdometerKm != null) {
        VehicleStamp(liveOdometerKm, vinHash, now, liveSource)
    } else {
        VehicleStamp(fallbackOdometerKm, vinHash, now, StampSource.MANUAL)
    }

    /**
     * A detected charge or refuel as a DRAFT record. The user only confirms the amount.
     * [unitPriceWon] is per kWh or per L; without it the amount stays empty for the user to fill.
     */
    fun energyDraft(
        id: String,
        vehicle: Vehicle,
        event: EnergyEvent,
        stamp: VehicleStamp,
        unitPriceWon: Double? = null,
        placeName: String? = null,
    ): Record {
        val amountWon = if (event.amount != null && unitPriceWon != null) (event.amount * unitPriceWon).roundToLong() else null
        val record = Record(
            id = id,
            vehicleId = vehicle.id,
            type = RecordType.ENERGY,
            occurredAt = event.endedAt,
            amountWon = amountWon,
            stamp = stamp,
            origin = RecordOrigin.DETECTED,
            status = RecordStatus.DRAFT,
            detail = RecordDetail.Energy(
                kind = event.kind,
                amount = event.amount,
                unitPriceWon = unitPriceWon,
                placeName = placeName,
                levelBefore = event.levelBefore,
                levelAfter = event.levelAfter,
            ),
        )
        return record.copy(integrityHash = integrityHash(record))
    }

    /**
     * The user confirmed a draft, possibly correcting the estimated amount. Confirming is the
     * user's first input, not an edit, so the hash is taken again and editedAt stays empty.
     * The stamp is kept from detection time.
     */
    fun confirm(draft: Record, amountWon: Long?): Record {
        require(draft.status == RecordStatus.DRAFT) { "Only drafts can be confirmed" }
        val confirmed = draft.copy(status = RecordStatus.CONFIRMED, amountWon = amountWon)
        return confirmed.copy(integrityHash = integrityHash(confirmed))
    }

    /**
     * SHA-256 over the stamp and the core fields. Kept from the first save; the signing scheme
     * for shared histories is decided with M5 (FEATURE_LOGIC 2.2).
     */
    fun integrityHash(record: Record): String {
        val s = record.stamp
        val text = listOf(
            record.id, record.vehicleId, record.type.name, record.occurredAt.toString(),
            record.amountWon?.toString().orEmpty(),
            s.odometerKm.toString(), s.vinHash.orEmpty(), s.capturedAt.toString(), s.source.name,
            record.detail.toString(),
        ).joinToString("|")
        return sha256(text)
    }

    /** For VINs: only the hash may be stored or sent (ARCHITECTURE 2.5-10). */
    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
