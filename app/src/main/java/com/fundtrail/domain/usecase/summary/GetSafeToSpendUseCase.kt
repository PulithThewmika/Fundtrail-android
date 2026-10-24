package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.MonthlySummary
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.model.SafeToSpendParts
import com.fundtrail.domain.model.SafeToSpendState
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.SpendingBreakdown
import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.income.ObserveSalaryExpectationUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * "How much can I spend this month without hurting the goal?" (#215; A-23, A-19, A-07; docs/design/safe-to-spend.md).
 *
 * `safeToSpend = received - committed - goal saving - discretionary`, all for the current month in [APP_ZONE]:
 * - received: actual income only, so expected or estimated income never counts (A-22) and a crypto loss lowers it.
 * - committed: committed expenses already recorded, plus the expected amount of every unpaused recurring template
 *   that falls due this month and is not yet confirmed, so a known bill is reserved before it is paid.
 * - goal saving: the active goal's `requiredMonthlyMinor`, or 0 without a goal.
 * - transfers are not income or expense in any of the inputs (A-21).
 *
 * Recurring templates are read from [RecurringRepository] rather than through `GetDueRecurringUseCase`, because
 * that one only lists items already due and this needs the ones due later in the month too.
 *
 * Pure maths lives in [compute] so every state is testable without flows. The result refreshes on any entry,
 * recurring confirm or goal change, and at midnight, since the month and days left depend on today.
 */
class GetSafeToSpendUseCase @Inject constructor(
    private val monthlySummary: GetMonthlySummaryUseCase,
    private val spendingBreakdown: GetSpendingBreakdownUseCase,
    private val goalProgress: GetGoalProgressUseCase,
    private val recurring: RecurringRepository,
    private val salaryStatus: GetSalaryStatusUseCase,
    private val salaryExpectation: ObserveSalaryExpectationUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<SafeToSpend> =
        dateChanges().flatMapLatest { today ->
            val month = YearMonth.from(today)
            val salary = combine(salaryStatus(month), salaryExpectation()) { status, expectation ->
                status to (expectation.expectedAmountMinor != null)
            }
            combine(
                monthlySummary(month),
                spendingBreakdown(month),
                goalProgress(),
                recurring.observeAll(),
                salary,
            ) { summary, breakdown, goal, templates, (status, salaryExpected) ->
                compute(
                    summary = summary,
                    breakdown = breakdown,
                    goalSavingMinor = goal?.requiredMonthlyMinor ?: 0L,
                    templates = templates,
                    salaryStatus = status,
                    salaryExpected = salaryExpected,
                    today = today,
                )
            }
        }

    /** Emits today (in [APP_ZONE]) now and again at each following midnight. Copied from `GetGoalProgressUseCase`. */
    private fun dateChanges(): Flow<LocalDate> = flow {
        val zoned = clock.withZone(APP_ZONE)
        while (true) {
            val today = LocalDate.now(zoned)
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant()
            delay(Duration.between(zoned.instant(), nextMidnight).toMillis().coerceAtLeast(1L))
        }
    }.distinctUntilChanged()

    companion object {
        /**
         * @param salaryExpected Whether an expected salary amount is set. Without one there is no salary to wait for.
         */
        internal fun compute(
            summary: MonthlySummary,
            breakdown: SpendingBreakdown,
            goalSavingMinor: Long,
            templates: List<RecurringTemplate>,
            salaryStatus: SalaryStatus,
            salaryExpected: Boolean,
            today: LocalDate,
        ): SafeToSpend {
            val month = YearMonth.from(today)
            val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1

            // A template whose cursor sits in an earlier month is a catch-up booked in that month, not this one.
            val reservedMinor = templates
                .filter { !it.isPaused && it.deletedAt == null && YearMonth.from(it.nextDueDate) == month }
                .sumOf { it.expectedAmountMinor }
            val committedMinor = breakdown.amountOf(SpendKind.COMMITTED) + reservedMinor
            val discretionaryMinor = breakdown.amountOf(SpendKind.DISCRETIONARY)

            val amountMinor = summary.receivedIncomeMinor - committedMinor - goalSavingMinor - discretionaryMinor

            val incomeRecorded = summary.incomeBySource.isNotEmpty()
            val state = when {
                !incomeRecorded && !salaryExpected -> SafeToSpendState.NOT_ENOUGH_DATA
                amountMinor > 0 -> SafeToSpendState.AVAILABLE
                salaryExpected && salaryStatus !is SalaryStatus.Received -> SafeToSpendState.WAITING_FOR_SALARY
                else -> SafeToSpendState.FULLY_USED
            }
            val perDayMinor = if (state == SafeToSpendState.AVAILABLE) {
                BigDecimal.valueOf(amountMinor)
                    .divide(BigDecimal.valueOf(daysLeft.toLong()), 0, RoundingMode.HALF_UP)
                    .toLong()
            } else {
                null
            }
            val parts = SafeToSpendParts(summary.receivedIncomeMinor, committedMinor, goalSavingMinor, discretionaryMinor)
            return SafeToSpend(amountMinor, perDayMinor, state, daysLeft, parts)
        }

        private fun SpendingBreakdown.amountOf(kind: SpendKind): Long =
            byNature.firstOrNull { it.spendKind == kind }?.amountMinor ?: 0L
    }
}
