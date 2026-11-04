package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The conversion numbers from #174 AC 3, plus the rounding edges. Every case is in minor units and micros,
 * which is how both figures are stored (ADR-0003).
 */
class FxConversionTest {

    @Test
    fun convertsTheAcceptanceCriteriaExample() {
        // 52.30 USD at 297.45 LKR per USD = LKR 15,556.64
        assertEquals(1_555_664L, FxConversion.lkrFromRate(5_230L, 297_450_000L))
    }

    @Test
    fun convertsTheDesignDocExample() {
        // USD 48.20 x 298.40 = LKR 14,382.88 (Docs/design/multi-currency.md)
        assertEquals(1_438_288L, FxConversion.lkrFromRate(4_820L, 298_400_000L))
    }

    @Test
    fun derivesTheRateFromTheLkrReceived() {
        // A clean payout round-trips exactly: 100.00 USD for LKR 29,745.00 is 297.45.
        assertEquals(297_450_000L, FxConversion.rateFromLkr(10_000L, 2_974_500L))
        assertEquals(2_974_500L, FxConversion.lkrFromRate(10_000L, 297_450_000L))
    }

    @Test
    fun derivesTheRateBehindTheAcceptanceCriteriaExample() {
        // LKR 15,556.64 for 52.30 USD is 297.450096 — the rounded LKR figure no longer divides evenly, so the
        // derived rate carries the remainder rather than silently reporting the 297.45 that was typed.
        assertEquals(297_450_096L, FxConversion.rateFromLkr(5_230L, 1_555_664L))
    }

    @Test
    fun roundsHalfCentsUp() {
        // 0.01 USD at 0.5 LKR = 0.005 LKR, which is half a cent: HALF_UP keeps it at one cent.
        assertEquals(1L, FxConversion.lkrFromRate(1L, 500_000L))
        assertEquals(2L, FxConversion.lkrFromRate(1L, 1_500_000L))
        // Just under half still rounds down, so the rule is HALF_UP and not "always up".
        assertEquals(0L, FxConversion.lkrFromRate(1L, 499_999L))
    }

    @Test
    fun roundsAHalfCentRateUp() {
        assertEquals(500_000L, FxConversion.rateFromLkr(2L, 1L))
        assertEquals(333_333L, FxConversion.rateFromLkr(3L, 1L))
    }

    @Test
    fun keepsTheSignOfARealisedCryptoLoss() {
        assertEquals(-1_555_664L, FxConversion.lkrFromRate(-5_230L, 297_450_000L))
        assertEquals(297_450_096L, FxConversion.rateFromLkr(-5_230L, -1_555_664L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsANonPositiveRate() {
        FxConversion.lkrFromRate(5_230L, 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAZeroOriginalAmount() {
        FxConversion.rateFromLkr(0L, 1_555_664L)
    }
}
