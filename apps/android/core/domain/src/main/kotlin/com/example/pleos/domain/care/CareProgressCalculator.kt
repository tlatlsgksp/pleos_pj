package com.example.pleos.domain.care

import com.example.pleos.domain.model.CareItemDef
import com.example.pleos.domain.model.CareItemState
import com.example.pleos.domain.model.CareProgress
import com.example.pleos.domain.model.CareStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * FEATURE_LOGIC L3-1. The only place that turns a care item into progress.
 * Screens and notifications must both call this, so they never disagree on what "a month" is.
 */
object CareProgressCalculator {

    const val DUE_SOON_RATIO = 0.8
    const val OVERDUE_RATIO = 1.0

    /**
     * @param currentOdometerKm always the vehicle's live value, never the last record's.
     * @param zone the zone the calendar months are counted in (the vehicle's local zone).
     */
    fun progress(
        def: CareItemDef,
        state: CareItemState?,
        currentOdometerKm: Double?,
        now: Instant,
        zone: ZoneId,
    ): CareProgress {
        val intervalKm = state?.customIntervalKm ?: def.intervalKm
        val intervalMonths = state?.customIntervalMonths ?: def.intervalMonths
        val lastKm = state?.lastServiceOdometerKm
        val lastAt = state?.lastServiceAt

        val distanceRatio = if (intervalKm != null && intervalKm > 0 && lastKm != null && currentOdometerKm != null) {
            (currentOdometerKm - lastKm).coerceAtLeast(0.0) / intervalKm
        } else {
            null
        }

        val lastDate = lastAt?.atZone(zone)?.toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        val elapsed = lastDate?.let { calendarMonthsBetween(it, today) }
        val monthsRatio = if (intervalMonths != null && intervalMonths > 0 && elapsed != null) {
            elapsed / intervalMonths
        } else {
            null
        }

        val ratio = listOfNotNull(distanceRatio, monthsRatio).maxOrNull()
        return CareProgress(
            itemId = def.id,
            status = statusOf(ratio),
            ratio = ratio,
            distanceRatio = distanceRatio,
            monthsRatio = monthsRatio,
            remainingKm = if (distanceRatio != null) intervalKm!! - (currentOdometerKm!! - lastKm!!) else null,
            elapsedMonths = elapsed?.toInt(),
            dueDate = if (lastDate != null && intervalMonths != null) lastDate.plusMonths(intervalMonths.toLong()) else null,
        )
    }

    fun statusOf(ratio: Double?): CareStatus = when {
        ratio == null -> CareStatus.UNKNOWN
        ratio >= OVERDUE_RATIO -> CareStatus.OVERDUE
        ratio >= DUE_SOON_RATIO -> CareStatus.DUE_SOON
        else -> CareStatus.OK
    }

    /**
     * Calendar months from [from] to [to], with the partial month as a fraction of that month's length.
     *
     * Whole months follow [LocalDate.plusMonths], which clamps to the month end: Jan 31 plus one
     * month is Feb 28. So on Feb 28 one whole month has passed, and the due date computed with
     * plusMonths and this count always agree. Returns 0 when [to] is before [from].
     */
    fun calendarMonthsBetween(from: LocalDate, to: LocalDate): Double {
        if (!to.isAfter(from)) return 0.0
        var whole = ChronoUnit.MONTHS.between(from, to)
        // ChronoUnit says 0 for Jan 31 -> Feb 28; plusMonths says Feb 28 is the one-month mark.
        while (!from.plusMonths(whole + 1).isAfter(to)) whole++
        val anchor = from.plusMonths(whole)
        val next = from.plusMonths(whole + 1)
        val fraction = ChronoUnit.DAYS.between(anchor, to).toDouble() / ChronoUnit.DAYS.between(anchor, next)
        return whole + fraction
    }
}
