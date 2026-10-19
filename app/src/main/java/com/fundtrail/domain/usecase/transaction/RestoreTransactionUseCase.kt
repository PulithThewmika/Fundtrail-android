package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.TransactionRepository
import javax.inject.Inject

/** Reverses a delete within the Undo snackbar window on the History screen (#109, FR-37). */
class RestoreTransactionUseCase @Inject constructor(private val transactions: TransactionRepository) {
    suspend operator fun invoke(id: String) = transactions.restore(id)
}
