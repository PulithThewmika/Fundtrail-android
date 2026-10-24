package com.fundtrail.domain.model

/** Which message the "Safe to spend" card shows (A-23, A-07). The UI picks the wording, not the use case. */
enum class SafeToSpendState {
    /** [SafeToSpend.amountMinor] is above zero. */
    AVAILABLE,

    /** Nothing left: [SafeToSpend.amountMinor] is zero or negative. */
    FULLY_USED,

    /** Nothing left, but the expected salary has not been recorded yet, so a negative number would mislead. */
    WAITING_FOR_SALARY,

    /** No income recorded this month and no salary expected, so there is nothing to compute from. */
    NOT_ENOUGH_DATA,
}

/**
 * What can still be spent this month without touching committed bills or the goal's monthly saving.
 * All amounts are LKR minor units.
 *
 * @property amountMinor `received - committed - goal saving - discretionary`; zero or negative once the month is used up.
 * @property perDayMinor [amountMinor] spread over [daysLeft], or null unless the state is [SafeToSpendState.AVAILABLE].
 * @property state See [SafeToSpendState].
 * @property daysLeft Days remaining in the month including today (Asia/Colombo); 1 on the last day.
 * @property parts The four numbers [amountMinor] was worked out from, for the explainer.
 */
data class SafeToSpend(
    val amountMinor: Long,
    val perDayMinor: Long?,
    val state: SafeToSpendState,
    val daysLeft: Int,
    val parts: SafeToSpendParts,
) {
    /** How far past zero the month is, 0 while there is money left. */
    val overByMinor: Long get() = if (amountMinor < 0) -amountMinor else 0L
}

/**
 * `amountMinor = receivedMinor - committedMinor - goalSavingMinor - discretionarySpentMinor`, LKR minor units.
 *
 * @property committedMinor Committed expenses already recorded plus recurring bills still due this month.
 */
data class SafeToSpendParts(
    val receivedMinor: Long,
    val committedMinor: Long,
    val goalSavingMinor: Long,
    val discretionarySpentMinor: Long,
)
