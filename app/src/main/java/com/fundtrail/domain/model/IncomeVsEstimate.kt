package com.fundtrail.domain.model

/**
 * Indicates whether the actual average income is below, within, or above the user's estimate range.
 */
enum class IncomeVsEstimateComparison {
    BELOW,
    WITHIN,
    ABOVE,
}

/**
 * Result of comparing trailing complete months average received income against user's estimate range (FR-58, A-16).
 *
 * @property comparison Comparison result: [IncomeVsEstimateComparison.BELOW], [IncomeVsEstimateComparison.WITHIN], or [IncomeVsEstimateComparison.ABOVE] (null if estimate not set)
 * @property actualAverageMinor Trailing complete months average received income, LKR minor units
 * @property estimateMinMinor Low end of user estimate range, LKR minor units
 * @property estimateMaxMinor High end of user estimate range, LKR minor units
 * @property monthsCount Number of complete months used to compute the average (0 to 3)
 */
data class IncomeVsEstimate(
    val comparison: IncomeVsEstimateComparison?,
    val actualAverageMinor: Long,
    val estimateMinMinor: Long? = null,
    val estimateMaxMinor: Long? = null,
    val monthsCount: Int = 0,
)
