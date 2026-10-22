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
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Computes [SalaryStatus] for a given month ([YearMonth]).
 *
 * Compares expected salary from [SettingsRepository] with actual salary deposits
 * from [TransactionRepository].
 */
class GetSalaryStatusUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(month: YearMonth): Flow<SalaryStatus> =
        combine(
            settingsRepository.observe(),
            transactionRepository.observeMonth(month),
        ) { settings, transactions ->
            compute(
                month = month,
                expectedSalaryMinor = settings.expectedSalaryMinor,
                salaryPayday = settings.salaryPayday,
                transactions = transactions,
                today = LocalDate.now(clock.withZone(APP_ZONE)),
            )
        }

    companion object {
        internal fun compute(
            month: YearMonth,
            expectedSalaryMinor: Long?,
            salaryPayday: Int,
            transactions: List<com.fundtrail.domain.model.Transaction>,
            today: LocalDate,
        ): SalaryStatus {
            val salaryTxs = transactions.filter {
                (it.status == TransactionStatus.ACTUAL) &&
                    (it.type == TransactionType.INCOME) &&
                    (it.incomeSource == IncomeSource.SALARY)
            }

            if (salaryTxs.isNotEmpty()) {
                val totalReceived = salaryTxs.sumOf { it.amountLkrMinor }
                val diff = expectedSalaryMinor?.let { totalReceived - it }
                return SalaryStatus.Received(
                    amountMinor = totalReceived,
                    differenceFromExpected = diff,
                )
            }

            val currentMonth = YearMonth.from(today)
            return when {
                month.isBefore(currentMonth) -> SalaryStatus.Due
                month.isAfter(currentMonth) -> SalaryStatus.NotDue
                else -> { // month == currentMonth
                    if (today.dayOfMonth >= salaryPayday) {
                        SalaryStatus.Due
                    } else {
                        SalaryStatus.NotDue
                    }
                }
            }
        }
    }
}
