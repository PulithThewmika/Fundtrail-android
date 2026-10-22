package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.SettingsRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/**
 * Observes expected vs received salary status for the given month (A-12, FR-49).
 */
class ObserveSalaryStatusUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    operator fun invoke(yearMonth: YearMonth = YearMonth.now(clock.withZone(APP_ZONE))): Flow<SalaryStatus> =
        combine(
            settingsRepository.observe(),
            transactionRepository.observeMonth(yearMonth)
        ) { settings, transactions ->
            val salaryTxs = transactions.filter {
                it.type == TransactionType.INCOME &&
                    it.status == TransactionStatus.ACTUAL &&
                    it.incomeSource == IncomeSource.SALARY &&
                    it.deletedAt == null
            }
            val isReceived = salaryTxs.isNotEmpty()
            val receivedAmount = if (isReceived) salaryTxs.sumOf { it.amountLkrMinor } else null
            val expected = settings.expectedSalaryMinor
            val variance = if (isReceived && expected != null && receivedAmount != null) {
                receivedAmount - expected
            } else {
                null
            }

            SalaryStatus(
                expectedAmountMinor = expected,
                payday = settings.salaryPayday,
                receivedAmountMinor = receivedAmount,
                isReceived = isReceived,
                varianceMinor = variance
            )
        }
}
