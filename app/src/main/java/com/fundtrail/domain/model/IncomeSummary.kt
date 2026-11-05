package com.fundtrail.domain.model

/**
 * FR-57: this month's received income, the trailing 3-month average, and the lowest-to-highest
 * range over the trailing 6 complete months (A-03: a month with no income counts as zero, not skipped).
 */
data class IncomeSummary(
    val thisMonthReceivedMinor: Long,
    val averageMinor: Long,
    val lowestMinor: Long,
    val highestMinor: Long,
)
