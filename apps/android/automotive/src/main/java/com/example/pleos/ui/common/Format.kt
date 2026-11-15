package com.example.pleos.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/**
 * Display formatting shared by the screens (design-foundation 3): a space between number and unit
 * except 원, thousands grouped (12,345). Screens format numbers only through here.
 */
object Format {
    private val date: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")

    /** Whole kilometres: odometer, range, remaining interval. */
    fun km(value: Double): String = "%,d km".format(value.roundToLong())

    /** A distance to somewhere: one decimal under 10 km, whole kilometres above. */
    fun distance(km: Double): String = if (km < 10) "%.1f km".format(km) else km(km)

    fun won(value: Long): String = "%,d원".format(value)

    fun kwh(value: Double): String = "%.1f kWh".format(value)

    fun liters(value: Double): String = "%.1f L".format(value)

    /** kWh for an EV, L for a fuel car. */
    fun energy(value: Double, isEv: Boolean): String = if (isEv) kwh(value) else liters(value)

    /** km/kWh for an EV, km/L for a fuel car. */
    fun efficiency(value: Double, isEv: Boolean): String = "%.1f %s".format(value, efficiencyUnit(isEv))

    /** The number of [efficiency] alone, for layouts that set the unit smaller beside it. */
    fun efficiencyNumber(value: Double): String = "%.1f".format(value)

    fun efficiencyUnit(isEv: Boolean): String = if (isEv) "km/kWh" else "km/L"

    fun percent(value: Double): String = "%d%%".format(value.roundToLong())

    fun date(value: LocalDate): String = value.format(date)

    fun shortDate(value: LocalDate): String = value.format(shortDate)
}
