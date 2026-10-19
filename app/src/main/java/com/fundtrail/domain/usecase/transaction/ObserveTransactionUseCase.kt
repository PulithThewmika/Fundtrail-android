package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Loads one transaction for edit mode in the quick-add sheet (#109). */
class ObserveTransactionUseCase @Inject constructor(private val transactions: TransactionRepository) {
    operator fun invoke(id: String): Flow<Transaction?> = transactions.observeById(id)
}
