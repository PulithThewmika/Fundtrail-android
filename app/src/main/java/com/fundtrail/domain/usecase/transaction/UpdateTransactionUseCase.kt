package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SameAccountTransferException
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
        val stored = transactions.observeById(edited.id).first()

        // A locked FX rate can't follow a changed amount without recomputing history (ADR-0003), and M2's
        // edit form has no foreign-currency field to update it with. TODO(M3): editing a foreign-currency entry.
        if (stored?.original != null && stored.amountLkrMinor != edited.amountLkrMinor) {
            return Result.Error(IllegalStateException("Editing the amount of a foreign-currency entry is not supported yet"))
        }

        // A same-account transfer edit would collapse to a no-op that still moves nothing and must be
        // rejected the same way AddTransferUseCase rejects it on create (FR-60, A-21).
        if (edited.type == TransactionType.TRANSFER && edited.accountId == edited.toAccountId) {
            return Result.Error(SameAccountTransferException())
        }

        val resolved = if (edited.type == TransactionType.EXPENSE) {
            val categoryId = edited.categoryId
                ?: return Result.Error(IllegalArgumentException("Expense requires categoryId"))
            // Keeping the entry's own category must still work once that category is archived or deleted
            // (#109); only switching to a *different* retired category is rejected.
            val keepsOwnCategory = stored?.categoryId == categoryId
            val category = categories.observeAll().first().firstOrNull { it.id == categoryId }
                ?: return Result.Error(IllegalArgumentException("Category not found: $categoryId"))
            if (!keepsOwnCategory && (category.archived || category.deletedAt != null)) {
                return Result.Error(IllegalArgumentException("Category is not active: $categoryId"))
            }
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
