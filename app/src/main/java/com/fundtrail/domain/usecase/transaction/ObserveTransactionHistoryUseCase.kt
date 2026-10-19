package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionDay
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject

/** Groups a month's transactions by day for the History screen (FR-100, SCR-11, #108). */
class ObserveTransactionHistoryUseCase @Inject constructor(private val transactions: TransactionRepository) {

    operator fun invoke(month: YearMonth, typeFilter: TransactionType? = null): Flow<List<TransactionDay>> =
        transactions.observeMonth(month).map { list ->
            val filtered = typeFilter?.let { type -> list.filter { it.type == type } } ?: list
            filtered
                .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(APP_ZONE).toLocalDate() }
                .toSortedMap(compareByDescending { it })
                .map { (date, dayTransactions) -> TransactionDay(date, dayTransactions, dayTransactions.netMinor()) }
        }

    private fun List<Transaction>.netMinor(): Long = sumOf {
        when {
            it.status != TransactionStatus.ACTUAL || it.type == TransactionType.TRANSFER -> 0L
            it.type == TransactionType.EXPENSE -> -it.amountLkrMinor
            else -> it.amountLkrMinor
        }
    }
}
