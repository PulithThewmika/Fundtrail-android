package com.fundtrail.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
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
        val date = LocalDate.of(2025, 1, 15)
        val epochMillis = date.atStartOfDay(java.time.ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli()
        val formatted = DateFormatter.format(epochMillis)
        assertEquals("15/01/2025", formatted)
    }

    @Test
    fun testTimezoneBoundaryColombo() {
        // 2026-10-07T20:00:00Z (8:00 PM UTC Oct 7, 2026)
        // In Asia/Colombo (UTC+5:30), this is 2026-10-08 01:30:00 AM (Oct 8, 2026)
        val epochMillis = Instant.parse("2026-10-07T20:00:00Z").toEpochMilli()
        val formatted = DateFormatter.format(epochMillis)
        assertEquals("08/10/2026", formatted)
    }
}
