package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Saves income that has actually been received (FR-45, A-16), so it counts toward the month summary.
 */
class AddIncomeUseCase @Inject constructor(private val transactions: TransactionRepository) {

    /**
     * [amountLkrMinor] is always the positive magnitude; the use case applies the sign. [isLoss] marks a
     * realised Crypto loss (schema.md §4.4, §6.1) and is rejected for any other source.
     *
     * [original] carries the foreign currency, its own positive magnitude and the rate locked at entry (#174);
     * an LKR entry passes null and stores no original block. [status] is EXPECTED when the LKR figure came from
     * a typed rate rather than the amount that actually landed (#176) — confirming it later is #177.
     * Returns the saved [Transaction] so the caller can offer Undo by its id.
     */
    suspend operator fun invoke(
        amountLkrMinor: Long,
        source: IncomeSource,
        accountId: String,
        occurredAt: Long,
        note: String? = null,
        isLoss: Boolean = false,
        original: OriginalAmount? = null,
        status: TransactionStatus = TransactionStatus.ACTUAL,
    ): Result<Transaction> {
        if (isLoss && source != IncomeSource.CRYPTO) {
            return Result.Error(IllegalArgumentException("Only Crypto income can be marked as a loss"))
        }
        val income = Transaction(
            id = "txn_${UUID.randomUUID()}",
            type = TransactionType.INCOME,
            status = status,
            amountLkrMinor = if (isLoss) -amountLkrMinor else amountLkrMinor,
            accountId = accountId,
            incomeSource = source,
            occurredAt = occurredAt,
            // The sign lives on both figures, so a loss reads the same whichever one is displayed (schema.md §4.4).
            original = original?.let { if (isLoss) it.copy(amountMinor = -it.amountMinor) else it },
            note = note?.trim()?.ifEmpty { null },
        )
        return when (val result = transactions.add(income)) {
            is Result.Success -> Result.Success(income)
            is Result.Error -> result
        }
    }
}
