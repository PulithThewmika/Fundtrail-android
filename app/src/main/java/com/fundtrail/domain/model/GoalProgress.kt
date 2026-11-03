package com.fundtrail.domain.model

import java.time.LocalDate

/**
 * Where a goal stands against its own pace, relative to [GoalProgress.requiredMonthlyMinor]
 * (schema §6.3, defined by #162).
 */
enum class GoalStatus {
    /** No complete month since the goal was created yet - pace can't be computed. */
    NOT_ENOUGH_DATA,
    AHEAD,
    ON_TRACK,
    BEHIND,
}

/**
 * Computed savings-goal progress (A-19, A-24): what Kavindu must save per month, what he is
 * actually saving, and whether the goal is still realistic.
 *
 * @property savedToDateMinor [Goal.baselineSavedMinor] plus actual net saving since the goal's
 *   creation month, LKR minor units.
 * @property remainingMinor `max(0, target - savedToDate)`.
 * @property requiredMonthlyMinor Amount needed per remaining month to still hit the deadline.
 * @property actualPaceMinor Average net saved over the last up-to-3 complete months since
 *   creation, or null before the first complete month ([GoalStatus.NOT_ENOUGH_DATA]).
 * @property status See [GoalStatus].
 * @property projectedDate Date the goal is reached at [actualPaceMinor], or null when the pace
 *   is null or <= 0 (the UI decides the wording, not this use case).
 * @property isInfeasible True when the required saving outstrips realistic income, or the pace
 *   can never close the gap.
 * @property suggestedDeadline A realistic deadline instead, only set when [isInfeasible].
 * @property suggestedTargetMinor A realistic target instead, only set when [isInfeasible].
 */
data class GoalProgress(
    val savedToDateMinor: Long,
    val remainingMinor: Long,
    val requiredMonthlyMinor: Long,
    val actualPaceMinor: Long?,
    val status: GoalStatus,
    val projectedDate: LocalDate?,
    val isInfeasible: Boolean,
    val suggestedDeadline: LocalDate?,
    val suggestedTargetMinor: Long?,
) {
    /** Distance from [requiredMonthlyMinor] per month: positive is behind, negative ahead. */
    val monthlyGapMinor: Long? get() = actualPaceMinor?.let { requiredMonthlyMinor - it }
}
