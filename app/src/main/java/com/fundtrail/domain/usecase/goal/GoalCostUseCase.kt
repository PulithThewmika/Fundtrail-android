package com.fundtrail.domain.usecase.goal

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

private val DAYS_PER_MONTH = BigDecimal(30)

/**
 * How many days of goal saving a spend costs (A-23, FR-41; #171): `amount / (requiredMonthly / 30)`,
 * rounded to one decimal, HALF_UP.
 */
class GoalCostUseCase @Inject constructor() {
    /** Null (hint hidden) when there is nothing meaningful to say: no required saving or no amount. */
    operator fun invoke(amountMinor: Long, requiredMonthlyMinor: Long): BigDecimal? {
        if (amountMinor <= 0L || requiredMonthlyMinor <= 0L) return null
        return BigDecimal(amountMinor).multiply(DAYS_PER_MONTH)
            .divide(BigDecimal(requiredMonthlyMinor), 1, RoundingMode.HALF_UP)
    }
}
