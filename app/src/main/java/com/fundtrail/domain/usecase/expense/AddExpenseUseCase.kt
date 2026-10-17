package com.fundtrail.domain.usecase.expense

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Saves an actual expense (FR-30, FR-31).
 *
 * The expense copies the committed/discretionary type onto itself at save time: [spendKind] when the user
 * overrode it, otherwise the category's `defaultSpendKind`. After that the expense never follows the
 * category again, so editing a category later cannot move past months between committed and discretionary.
 */
class AddExpenseUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) {
    /** Returns the saved [Transaction] so the caller can offer Undo by its id. */
    suspend operator fun invoke(
        amountLkrMinor: Long,
        categoryId: String,
        accountId: String,
        occurredAt: Long,
        note: String? = null,
        spendKind: SpendKind? = null,
    ): Result<Transaction> {
        val category = categories.observeAll().first().firstOrNull { it.id == categoryId && it.deletedAt == null }
            ?: return Result.Error(IllegalArgumentException("Category not found: $categoryId"))
        if (category.archived) return Result.Error(IllegalArgumentException("Category is archived: $categoryId"))

        val expense = Transaction(
            id = "txn_${UUID.randomUUID()}",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = amountLkrMinor,
            accountId = accountId,
            categoryId = category.id,
            spendKind = spendKind ?: category.defaultSpendKind,
            occurredAt = occurredAt,
            note = note?.trim()?.ifEmpty { null },
        )
        return when (val result = transactions.add(expense)) {
            is Result.Success -> Result.Success(expense)
            is Result.Error -> result
        }
    }
}
