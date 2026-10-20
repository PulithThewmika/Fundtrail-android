package com.fundtrail.domain.usecase.goal

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.GoalProgress
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.domain.model.MonthlySummary
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Pace is AHEAD at >=110% of required, ON_TRACK at >=90%, BEHIND below that (ADR-0006). */
private val AHEAD_FACTOR = BigDecimal("1.1")
private val ON_TRACK_FACTOR = BigDecimal("0.9")

/** A goal is infeasible once it needs more than half of trailing average income (ADR-0006). */
private val INFEASIBLE_INCOME_SHARE = BigDecimal("0.5")

/** Fallback saving rate used to suggest a deadline when the current pace can't get there. */
private val FALLBACK_INCOME_SHARE = BigDecimal("0.3")

private const val PACE_WINDOW_MONTHS = 3L

/**
 * Computes [GoalProgress] for the active goal (A-19, A-24; schema §6.3; #162).
 *
 * Pure maths lives in [compute] so it's testable without a [Flow] of summaries; this class's
 * job is wiring the right months of [GetMonthlySummaryUseCase] to it.
 */
class GetGoalProgressUseCase @Inject constructor(
    private val goals: GoalRepository,
    private val monthlySummary: GetMonthlySummaryUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<GoalProgress?> =
        goals.observeActive().flatMapLatest { goal ->
            if (goal == null) {
                flowOf(null)
            } else {
                val today = LocalDate.now(clock.withZone(APP_ZONE))
                val currentMonth = YearMonth.from(today)
                val months = monthsNeeded(goal, currentMonth)
                combine(months.map { monthlySummary(it) }) { summaries ->
                    compute(goal, months.zip(summaries.toList()).toMap(), today)
                }
            }
        }

    companion object {
        /** Every month from min(creation, pace window start) to the current month, inclusive. */
        private fun monthsNeeded(goal: Goal, currentMonth: YearMonth): List<YearMonth> {
            val creationMonth = goal.createdAt
                ?.let { YearMonth.from(java.time.Instant.ofEpochMilli(it).atZone(APP_ZONE)) }
                ?: currentMonth
            val paceWindowStart = currentMonth.minusMonths(PACE_WINDOW_MONTHS)
            val start = minOf(creationMonth, paceWindowStart)
            return generateSequence(start) { it.plusMonths(1) }
                .takeWhile { !it.isAfter(currentMonth) }
                .toList()
        }

        /** Pure projection maths (ADR-0006). [summaries] must cover every month [monthsNeeded] asked for. */
        internal fun compute(
            goal: Goal,
            summaries: Map<YearMonth, MonthlySummary>,
            today: LocalDate,
        ): GoalProgress {
            val currentMonth = YearMonth.from(today)
            val creationMonth = goal.createdAt
                ?.let { YearMonth.from(java.time.Instant.ofEpochMilli(it).atZone(APP_ZONE)) }
                ?: currentMonth

            val savedToDate = goal.baselineSavedMinor + monthsFrom(creationMonth, currentMonth)
                .sumOf { summaries[it]?.netSavedMinor ?: 0L }
            val remaining = (goal.targetLkrMinor - savedToDate).coerceAtLeast(0L)

            val deadlineMonth = YearMonth.from(goal.deadline)
            val monthsLeft = monthsBetweenInclusive(currentMonth, deadlineMonth).coerceAtLeast(1L)
            val requiredMonthly = ceilDiv(remaining, monthsLeft)

            val completeMonths = monthsFrom(creationMonth, currentMonth.minusMonths(1))
                .filter { !it.isBefore(creationMonth) }
                .takeLast(PACE_WINDOW_MONTHS.toInt())
            val actualPace = if (completeMonths.isEmpty()) {
                null
            } else {
                completeMonths.sumOf { summaries[it]?.netSavedMinor ?: 0L } / completeMonths.size
            }

            val status = when {
                actualPace == null -> GoalStatus.NOT_ENOUGH_DATA
                actualPace >= bd(requiredMonthly).multiply(AHEAD_FACTOR).toLong(RoundingMode.CEILING) -> GoalStatus.AHEAD
                actualPace >= bd(requiredMonthly).multiply(ON_TRACK_FACTOR).toLong(RoundingMode.CEILING) -> GoalStatus.ON_TRACK
                else -> GoalStatus.BEHIND
            }

            val projectedDate = if (actualPace != null && actualPace > 0 && remaining > 0) {
                today.plusMonths(ceilDiv(remaining, actualPace))
            } else if (actualPace != null && remaining <= 0L) {
                today
            } else {
                null
            }

            val trailingIncome = completeMonths
                .ifEmpty { null }
                ?.let { months -> months.sumOf { summaries[it]?.receivedIncomeMinor ?: 0L } / months.size }

            val exceedsIncome = trailingIncome != null &&
                bd(requiredMonthly) > bd(trailingIncome).multiply(INFEASIBLE_INCOME_SHARE)
            val paceCantFinish = remaining > 0 && actualPace != null && actualPace <= 0
            val isInfeasible = remaining > 0 && (exceedsIncome || paceCantFinish)

            val suggestedDeadline: LocalDate?
            val suggestedTarget: Long?
            if (isInfeasible) {
                val suggestionPace = if (actualPace != null && actualPace > 0) {
                    actualPace
                } else {
                    trailingIncome?.let { bd(it).multiply(FALLBACK_INCOME_SHARE).toLong(RoundingMode.HALF_UP) }
                }
                suggestedDeadline = suggestionPace?.takeIf { it > 0 }
                    ?.let { today.plusMonths(ceilDiv(remaining, it)) }
                suggestedTarget = if (actualPace != null && actualPace > 0) {
                    savedToDate + actualPace * monthsLeft
                } else {
                    null
                }
            } else {
                suggestedDeadline = null
                suggestedTarget = null
            }

            return GoalProgress(
                savedToDateMinor = savedToDate,
                remainingMinor = remaining,
                requiredMonthlyMinor = requiredMonthly,
                actualPaceMinor = actualPace,
                status = status,
                projectedDate = projectedDate,
                isInfeasible = isInfeasible,
                suggestedDeadline = suggestedDeadline,
                suggestedTargetMinor = suggestedTarget,
            )
        }

        private fun monthsFrom(start: YearMonth, endInclusive: YearMonth): List<YearMonth> =
            if (endInclusive.isBefore(start)) {
                emptyList()
            } else {
                generateSequence(start) { it.plusMonths(1) }.takeWhile { !it.isAfter(endInclusive) }.toList()
            }

        private fun monthsBetweenInclusive(from: YearMonth, to: YearMonth): Long =
            java.time.temporal.ChronoUnit.MONTHS.between(from, to) + 1

        private fun ceilDiv(amount: Long, divisor: Long): Long =
            bd(amount).divide(bd(divisor), 0, RoundingMode.CEILING).toLong()

        private fun bd(value: Long): BigDecimal = BigDecimal(value)

        private fun BigDecimal.toLong(rounding: RoundingMode): Long = setScale(0, rounding).toLong()
    }
}
