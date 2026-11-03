package com.fundtrail.domain.usecase.goal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class GoalCostUseCaseTest {
    private val goalCost = GoalCostUseCase()

    @Test
    fun spendIsConvertedToDaysOfRequiredSaving() {
        // LKR 24,000 against LKR 12,000 a month: 24000 / (12000 / 30) = 60 days.
        assertEquals(BigDecimal("60.0"), goalCost(2_400_000L, 1_200_000L))
        // LKR 1,500 against LKR 25,000 a month: 1.8 days.
        assertEquals(BigDecimal("1.8"), goalCost(150_000L, 2_500_000L))
    }

    @Test
    fun roundsToOneDecimalHalfUp() {
        // 1 * 30 / 400 = 0.075 -> 0.1 (HALF_UP); 1 * 30 / 600 = 0.05 -> 0.1 as well.
        assertEquals(BigDecimal("0.1"), goalCost(1L, 400L))
        assertEquals(BigDecimal("0.1"), goalCost(1L, 600L))
        // 1 * 30 / 1000 = 0.03 -> 0.0
        assertEquals(BigDecimal("0.0"), goalCost(1L, 1_000L))
    }

    @Test
    fun hiddenWhenRequiredMonthlyIsZeroOrNegative() {
        assertNull(goalCost(150_000L, 0L))
        assertNull(goalCost(150_000L, -1L))
    }

    @Test
    fun hiddenWhenAmountIsZeroOrNegative() {
        assertNull(goalCost(0L, 2_500_000L))
        assertNull(goalCost(-100L, 2_500_000L))
    }
}
