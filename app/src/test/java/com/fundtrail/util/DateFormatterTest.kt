package com.fundtrail.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateFormatterTest {

    @Test
    fun testDateFormatting() {
        val date = LocalDate.of(2026, 10, 8)
        val formatted = DateFormatter.format(date)
        assertEquals("08/10/2026", formatted)
    }

    @Test
    fun testEpochMillisFormatting() {
        // 2026-10-08 00:00:00 UTC timestamp in millis approx, or testing via LocalDate
        val date = LocalDate.of(2025, 1, 15)
        val epochMillis = date.atStartOfDay(java.time.ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli()
        val formatted = DateFormatter.format(epochMillis)
        assertEquals("15/01/2025", formatted)
    }
}
