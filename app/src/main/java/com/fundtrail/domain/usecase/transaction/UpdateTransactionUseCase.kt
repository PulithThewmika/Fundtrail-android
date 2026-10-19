package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Saves edits from the quick-add sheet in edit mode (#109): amount, account, date (back-dating moves the
 * entry to the correct month, FR-35), note, and for an expense the category and spend-kind override.
 *
 * Mirrors [com.fundtrail.domain.usecase.expense.AddExpenseUseCase]'s category rule: an expense takes the
 * override when the user chose one, otherwise the (possibly new) category's `defaultSpendKind`.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(edited: Transaction, spendKindOverride: SpendKind? = null): Result<Transaction> {
        val resolved = if (edited.type == TransactionType.EXPENSE) {
            val categoryId = edited.categoryId
                ?: return Result.Error(IllegalArgumentException("Expense requires categoryId"))
            val category = categories.observeAll().first().firstOrNull { it.id == categoryId && it.deletedAt == null }
                ?: return Result.Error(IllegalArgumentException("Category not found: $categoryId"))
            if (category.archived) return Result.Error(IllegalArgumentException("Category is archived: $categoryId"))
            edited.copy(spendKind = spendKindOverride ?: category.defaultSpendKind)
        } else {
            edited
        }
        return when (val result = transactions.update(resolved)) {
            is Result.Success -> Result.Success(resolved)
            is Result.Error -> result
        }
    }
}
