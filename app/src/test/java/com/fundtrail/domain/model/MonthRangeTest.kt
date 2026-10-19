package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

class MonthRangeTest {

    private fun colomboMillis(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(APP_ZONE).toInstant().toEpochMilli()

    private fun Pair<Long, Long>.contains(millis: Long) = millis >= first && millis < second

    @Test
    fun `entry at 23-59 on last day belongs to that month, midnight belongs to the next`() {
        val oct = YearMonth.of(2026, 10).toMillisRange()
        val nov = YearMonth.of(2026, 11).toMillisRange()
        val lastMinuteOfOct = colomboMillis(2026, 10, 31, 23, 59)
        val midnightNov1 = colomboMillis(2026, 11, 1, 0, 0)

        assertTrue(oct.contains(lastMinuteOfOct))
        assertFalse(nov.contains(lastMinuteOfOct))
        assertFalse(oct.contains(midnightNov1))
        assertTrue(nov.contains(midnightNov1))
    }

    @Test
    fun `bounds are Colombo midnight, not UTC`() {
        val (start, end) = YearMonth.of(2026, 10).toMillisRange()
        // Colombo is UTC+05:30, so local midnight is 18:30 UTC the previous day.
        assertEquals(java.time.Instant.parse("2026-09-30T18:30:00Z").toEpochMilli(), start)
        assertEquals(java.time.Instant.parse("2026-10-31T18:30:00Z").toEpochMilli(), end)
    }

    @Test
    fun `adjacent months share a boundary with no gap or overlap`() {
        assertEquals(YearMonth.of(2026, 12).toMillisRange().second, YearMonth.of(2027, 1).toMillisRange().first)
    }
}
