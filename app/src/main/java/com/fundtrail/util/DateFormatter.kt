package com.fundtrail.util

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Date formatting utility for FundTrail.
 * Formats dates as DD/MM/YYYY as specified in SRS / EPIC-006/T4 S2.
 */
object DateFormatter {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val zoneId: ZoneId = ZoneId.of("Asia/Colombo")

    fun format(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return formatter.format(localDate)
    }

    fun format(date: Date): String {
        return format(date.time)
    }

    fun format(timestamp: Timestamp): String {
        return format(timestamp.toDate())
    }

    fun format(localDate: LocalDate): String {
        return formatter.format(localDate)
    }
}
