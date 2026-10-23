package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeVsEstimate
import com.fundtrail.domain.model.IncomeVsEstimateComparison
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.repository.SettingsRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject

/**
 * Calculates trailing 3 complete months average received income and compares it against the user's estimate (FR-58, A-16).
 *
 * Gap-tolerant (A-03): complete months with 0 income are included in the average.
 */
class GetIncomeVsEstimateUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(currentMonth: YearMonth = YearMonth.now(clock.withZone(APP_ZONE))): Flow<IncomeVsEstimate> {
        val m1 = currentMonth.minusMonths(1)
        val m2 = currentMonth.minusMonths(2)
        val m3 = currentMonth.minusMonths(3)

        return combine(
            settingsRepository.observe(),
            transactionRepository.observeMonth(m1),
            transactionRepository.observeMonth(m2),
            transactionRepository.observeMonth(m3),
        ) { settings, m1Txs, m2Txs, m3Txs ->
            compute(
                settings = settings,
                month1Txs = m1Txs,
                month2Txs = m2Txs,
                month3Txs = m3Txs,
                currentMonth = currentMonth,
            )
        }
    }

    companion object {
        internal fun compute(
            settings: UserSettings,
            month1Txs: List<Transaction>,
            month2Txs: List<Transaction>,
            month3Txs: List<Transaction>,
            currentMonth: YearMonth,
        ): IncomeVsEstimate {
            val m1 = currentMonth.minusMonths(1)
            val m2 = currentMonth.minusMonths(2)

            val availableMonthsCount = if (settings.createdAt != null) {
                val creationMonth = YearMonth.from(Instant.ofEpochMilli(settings.createdAt).atZone(APP_ZONE))
                when {
                    creationMonth.isAfter(m1) -> 0
                    creationMonth == m1 -> 1
                    creationMonth == m2 -> 2
                    else -> 3
                }
            } else {
                when {
                    hasReceivedIncome(month3Txs) -> 3
                    hasReceivedIncome(month2Txs) -> 2
                    hasReceivedIncome(month1Txs) -> 1
                    else -> if (month1Txs.isNotEmpty() || month2Txs.isNotEmpty() || month3Txs.isNotEmpty()) 3 else 0
                }
            }

            val m1Received = sumReceivedIncome(month1Txs)
            val m2Received = sumReceivedIncome(month2Txs)
            val m3Received = sumReceivedIncome(month3Txs)

            val (totalReceived, count) = when (availableMonthsCount) {
                1 -> Pair(m1Received, 1)
                2 -> Pair(m1Received + m2Received, 2)
                3 -> Pair(m1Received + m2Received + m3Received, 3)
                else -> Pair(0L, 0)
            }

            val actualAverageMinor = if (count > 0) totalReceived / count else 0L

            val min = settings.incomeEstimateMinMinor
            val max = settings.incomeEstimateMaxMinor

            val comparison = when {
                (min == null) && (max == null) -> null
                min != null && actualAverageMinor < min -> IncomeVsEstimateComparison.BELOW
                max != null && actualAverageMinor > max -> IncomeVsEstimateComparison.ABOVE
                else -> IncomeVsEstimateComparison.WITHIN
            }

            return IncomeVsEstimate(
                comparison = comparison,
                actualAverageMinor = actualAverageMinor,
                estimateMinMinor = min,
                estimateMaxMinor = max,
                monthsCount = count,
            )
        }

        private fun hasReceivedIncome(txs: List<Transaction>): Boolean =
            txs.any { it.status == TransactionStatus.ACTUAL && it.type == TransactionType.INCOME && it.deletedAt == null }

        private fun sumReceivedIncome(txs: List<Transaction>): Long =
            txs.filter { it.status == TransactionStatus.ACTUAL && it.type == TransactionType.INCOME && it.deletedAt == null }
                .sumOf { it.amountLkrMinor }
    }
}
