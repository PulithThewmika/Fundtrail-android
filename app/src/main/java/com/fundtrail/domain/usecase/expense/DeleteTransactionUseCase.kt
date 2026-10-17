package com.fundtrail.domain.usecase.expense

import com.fundtrail.domain.repository.TransactionRepository
import javax.inject.Inject

/** Soft-deletes a transaction; quick-add uses it for Undo right after a save (FR-37). */
class DeleteTransactionUseCase @Inject constructor(private val transactions: TransactionRepository) {
    suspend operator fun invoke(id: String) = transactions.delete(id)
}
