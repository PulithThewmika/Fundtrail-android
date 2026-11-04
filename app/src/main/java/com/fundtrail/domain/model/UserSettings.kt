package com.fundtrail.domain.model

/**
 * Domain model for per-user settings, stored on the `users/{uid}` document.
 *
 * @property incomeEstimateMinMinor The user's own monthly income estimate (low end), LKR minor units
 * @property incomeEstimateMaxMinor High end of the estimate, LKR minor units
 * @property expectedSalaryMinor Expected monthly salary, LKR minor units
 * @property salaryPayday Day of month the salary is expected, 1-28
 */
data class UserSettings(
    val incomeEstimateMinMinor: Long? = null,
    val incomeEstimateMaxMinor: Long? = null,
    val expectedSalaryMinor: Long? = null,
    val salaryPayday: Int = 25,
) {
    /**
     * Validates settings invariants:
     * - When both estimates are set, [incomeEstimateMinMinor] must be <= [incomeEstimateMaxMinor].
     * - [salaryPayday] must be between 1 and 28.
     */
    fun validate() {
        val min = incomeEstimateMinMinor
        val max = incomeEstimateMaxMinor
        if (min != null && max != null) {
            require(min <= max) { "Income estimate min must not exceed max" }
        }
        require(salaryPayday in 1..28) { "Salary payday must be between 1 and 28" }
    }
}
