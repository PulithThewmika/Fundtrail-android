package com.fundtrail.domain.usecase.transfer

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SameAccountTransferException
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Moves money between two of the user's own accounts (FR-60, A-21). A transfer is always `ACTUAL` and is
 * neither income nor expense, so it changes account balances (and goal progress) but never the month totals.
 */
class AddTransferUseCase @Inject constructor(private val transactions: TransactionRepository) {

    /** Returns the saved [Transaction] so the caller can offer Undo by its id. */
    suspend operator fun invoke(
        amountLkrMinor: Long,
        fromAccountId: String,
        toAccountId: String,
        occurredAt: Long,
        note: String? = null,
    ): Result<Transaction> {
        if (fromAccountId == toAccountId) return Result.Error(SameAccountTransferException())
        val transfer = Transaction(
            id = "txn_${UUID.randomUUID()}",
            type = TransactionType.TRANSFER,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = amountLkrMinor,
            accountId = fromAccountId,
            toAccountId = toAccountId,
            occurredAt = occurredAt,
            note = note?.trim()?.ifEmpty { null },
        )
        return when (val result = transactions.add(transfer)) {
            is Result.Success -> Result.Success(transfer)
            is Result.Error -> result
        }
    }
}
