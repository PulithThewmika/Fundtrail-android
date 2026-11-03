package com.fundtrail.domain.model

/**
 * What the user told the app to expect (A-12): the amount, and the day of month it arrives.
 * Separate from [SalaryStatus] because the status says where this month stands, while the Income screen also
 * shows the expectation before payday, when [SalaryStatus.NotDue] carries neither number.
 *
 * @property expectedAmountMinor LKR minor units, or null when no expected salary is set.
 * @property payday Day of month (1-28).
 */
data class SalaryExpectation(
    val expectedAmountMinor: Long?,
    val payday: Int,
)
