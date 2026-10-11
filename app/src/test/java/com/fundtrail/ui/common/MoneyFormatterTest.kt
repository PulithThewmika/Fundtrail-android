package com.fundtrail.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun testFormatPositiveAmount() {
        // LKR 1,000.50 represented in minor units (100050 cents)
        val result = MoneyFormatter.formatLkr(100050L)
        assertEquals("LKR 1,000.50", result)
    }

    @Test
    fun testFormatZeroAmount() {
        val result = MoneyFormatter.formatLkr(0L)
        assertEquals("LKR 0.00", result)
    }

    @Test
    fun testFormatNegativeAmount() {
        val result = MoneyFormatter.formatLkr(-100050L)
        assertEquals("-LKR 1,000.50", result)
    }

    @Test
    fun testFormatSmallAmount() {
        val result = MoneyFormatter.formatLkr(50L)
        assertEquals("LKR 0.50", result)
    }

    @Test
    fun testFormatLargeAmount() {
        val result = MoneyFormatter.formatLkr(160000000L) // LKR 1,600,000.00
        assertEquals("LKR 1,600,000.00", result)
    }

    @Test
    fun testFormatExceedsDoublePrecision() {
        // Value exceeding Double's exact 53-bit integer mantissa: 9007199254740993L
        // Double division 9007199254740993L / 100.0 produces 90071992547409.92 (off by 1 cent)
        // BigDecimal keeps exact value: LKR 90,071,992,547,409.93
        val result = MoneyFormatter.formatLkr(9007199254740993L)
        assertEquals("LKR 90,071,992,547,409.93", result)
    }
}
