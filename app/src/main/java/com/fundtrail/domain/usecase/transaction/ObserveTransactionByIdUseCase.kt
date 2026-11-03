package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes a single transaction by ID.
 */
class ObserveTransactionByIdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(id: String): Flow<Transaction?> = transactionRepository.observeById(id)
}
