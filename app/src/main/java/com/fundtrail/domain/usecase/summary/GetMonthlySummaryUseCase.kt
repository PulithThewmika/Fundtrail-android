package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.MonthlySummary
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlySummaryUseCase @Inject constructor(private val transactions: TransactionRepository) {
    operator fun invoke(month: YearMonth): Flow<MonthlySummary> =
        transactions.observeMonth(month).map { all ->
            val actual = all.filter { it.status == TransactionStatus.ACTUAL }
            val income = actual.filter { it.type == TransactionType.INCOME }
            val received = income.sumOf { it.amountLkrMinor }
            val spent = actual.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountLkrMinor }
            MonthlySummary(
                receivedIncomeMinor = received,
                spentMinor = spent,
                netSavedMinor = received - spent,
                incomeBySource = income.groupBy { it.incomeSource!! }.mapValues { (_, txs) -> txs.sumOf { it.amountLkrMinor } },
            )
        }
}
