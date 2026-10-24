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
 * Observes the signed realised crypto income from 1 January through the current instant.
 */
class GetCryptoYtdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    operator fun invoke(yearMonth: YearMonth = YearMonth.now(clock.withZone(APP_ZONE))): Flow<CryptoYtd> {
        val currentYear = yearMonth.year
        val months = (1..yearMonth.monthValue).map { YearMonth.of(currentYear, it) }
        val flows = months.map { transactionRepository.observeMonth(it) }

        return combine(flows) { monthLists ->
            // Read the cutoff per emission, not once at subscription. A transaction saved
            // while the Income screen is open is stamped at the current time, so a cutoff
            // captured earlier would filter it out of the emission it triggered.
            val endExclusive = if (yearMonth == YearMonth.now(clock.withZone(APP_ZONE))) {
                clock.instant().toEpochMilli()
            } else {
                yearMonth.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli()
            }
            val cryptoEntries = monthLists
                .asSequence()
                .flatMap { it.asSequence() }
                .filter {
                    it.type == TransactionType.INCOME &&
                        it.status == TransactionStatus.ACTUAL &&
                        it.incomeSource == IncomeSource.CRYPTO &&
                        it.deletedAt == null &&
                        it.occurredAt < endExclusive
                }
                .toList()
            CryptoYtd(
                year = currentYear,
                netLkrMinor = cryptoEntries.sumOf { it.amountLkrMinor },
                transactionCount = cryptoEntries.size
            )
        }
    }
}
