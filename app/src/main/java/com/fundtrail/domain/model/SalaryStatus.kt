package com.fundtrail.domain.model

/**
 * Salary status model for a given month.
 *
 * Used by GetSalaryStatusUseCase to indicate whether salary is NOT_DUE, DUE, or RECEIVED.
 */
sealed interface SalaryStatus {
    data object NotDue : SalaryStatus
    data class Due(
        val expectedAmountMinor: Long?,
        val salaryPayday: Int,
    ) : SalaryStatus

    /**
     * Received salary status for a given month.
     * Sums every salary transaction in the month ("two salary entries in one month" case).
     */
    data class Received(
        val amountMinor: Long,
        val differenceFromExpected: Long?,
    ) : SalaryStatus
}
