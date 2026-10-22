package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.CryptoYtd
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/**
 * Observes the running year-to-date net position for realised crypto results (A-15, FR-55).
 */
class ObserveCryptoYtdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    operator fun invoke(yearMonth: YearMonth = YearMonth.now(clock.withZone(APP_ZONE))): Flow<CryptoYtd> {
        val currentYear = yearMonth.year
        val months = (1..yearMonth.monthValue).map { YearMonth.of(currentYear, it) }
        val flows = months.map { transactionRepository.observeMonth(it) }

        return combine(flows) { monthLists ->
            val allYearTxs = monthLists.flatMap { it }.filter {
                it.type == TransactionType.INCOME &&
                    it.status == TransactionStatus.ACTUAL &&
                    it.incomeSource == IncomeSource.CRYPTO &&
                    it.deletedAt == null
            }
            val netMinor = allYearTxs.sumOf { it.amountLkrMinor }
            CryptoYtd(
                year = currentYear,
                netLkrMinor = netMinor,
                transactionCount = allYearTxs.size
            )
        }
    }
}
