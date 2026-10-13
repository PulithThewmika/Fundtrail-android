package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.CategoryShare
import com.fundtrail.domain.model.NatureShare
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.SpendingBreakdown
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class GetSpendingBreakdownUseCase @Inject constructor(private val transactions: TransactionRepository) {
    operator fun invoke(month: YearMonth): Flow<SpendingBreakdown> =
        transactions.observeMonth(month).map { all ->
            // spendKind is the snapshot taken at entry, not the category's current default (schema §3.4).
            val expenses = all.filter { it.status == TransactionStatus.ACTUAL && it.type == TransactionType.EXPENSE }
            val total = expenses.sumOf { it.amountLkrMinor }
            fun percent(amount: Long) = if (total == 0L) 0.0 else amount * 100.0 / total

            SpendingBreakdown(
                totalSpentMinor = total,
                byNature = SpendKind.entries.map { kind ->
                    val amount = expenses.filter { it.spendKind == kind }.sumOf { it.amountLkrMinor }
                    NatureShare(kind, amount, percent(amount))
                },
                categories = expenses
                    .groupBy { it.categoryId!! to it.spendKind!! }
                    .map { (key, txs) ->
                        val amount = txs.sumOf { it.amountLkrMinor }
                        CategoryShare(key.first, key.second, amount, percent(amount))
                    }
                    .sortedWith(compareByDescending<CategoryShare> { it.amountMinor }.thenBy { it.categoryId }),
            )
        }
}
