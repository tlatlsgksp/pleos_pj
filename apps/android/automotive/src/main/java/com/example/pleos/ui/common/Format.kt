package com.example.pleos.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/** Display formatting shared by the screens. Korean grouping: 12,345. */
object Format {
    private val date: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun km(value: Double): String = "%,d km".format(value.roundToLong())

    fun won(value: Long): String = "%,d원".format(value)

    fun kwh(value: Double): String = "%.1f kWh".format(value)

    fun percent(value: Double): String = "%d%%".format(value.roundToLong())

    fun date(value: LocalDate): String = value.format(date)
}
