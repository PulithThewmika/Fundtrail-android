package com.fundtrail.domain.model

/**
 * Expected vs received salary status for the current month (A-12, FR-49).
 *
 * @property expectedAmountMinor Expected salary amount in LKR minor units, from [UserSettings.expectedSalaryMinor]
 * @property payday Expected day of month (1-28), from [UserSettings.salaryPayday]
 * @property receivedAmountMinor Actual salary received this month in LKR minor units, or null if none
 * @property isReceived Whether salary has been received this month
 * @property varianceMinor Difference (received - expected) when both are present, showing bonus or shortfall
 */
data class SalaryStatus(
    val expectedAmountMinor: Long?,
    val payday: Int,
    val receivedAmountMinor: Long?,
    val isReceived: Boolean,
    val varianceMinor: Long?
)
