package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Saves income that has actually been received (FR-45, A-16), so it counts toward the month summary.
 * TODO(M3): negative amounts for realised crypto losses (schema.md section 4.4, A-15).
 */
class AddIncomeUseCase @Inject constructor(private val transactions: TransactionRepository) {

    /** Returns the saved [Transaction] so the caller can offer Undo by its id. */
    suspend operator fun invoke(
        amountLkrMinor: Long,
        source: IncomeSource,
        accountId: String,
        occurredAt: Long,
        note: String? = null,
    ): Result<Transaction> {
        val income = Transaction(
            id = "txn_${UUID.randomUUID()}",
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = amountLkrMinor,
            accountId = accountId,
            incomeSource = source,
            occurredAt = occurredAt,
            note = note?.trim()?.ifEmpty { null },
        )
        return when (val result = transactions.add(income)) {
            is Result.Success -> Result.Success(income)
            is Result.Error -> result
        }
    }
}
