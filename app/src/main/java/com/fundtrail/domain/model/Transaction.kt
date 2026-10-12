package com.fundtrail.domain.model

/**
 * Domain model representing a financial transaction (Expense, Income, or Transfer).
 *
 * All amounts are stored in LKR minor units ([amountLkrMinor]) as the ground truth.
 *
 * @property id Unique transaction ID
 * @property type Discriminates [TransactionType.EXPENSE], [TransactionType.INCOME], or [TransactionType.TRANSFER]
 * @property status [TransactionStatus.EXPECTED] or [TransactionStatus.ACTUAL] (default ACTUAL)
 * @property amountLkrMinor Ground truth amount in LKR minor units
 * @property accountId Source or primary account ID
 * @property toAccountId Destination account ID (required for TRANSFER)
 * @property categoryId Associated expense category ID (required for EXPENSE)
 * @property spendKind Nature of spending: COMMITTED or DISCRETIONARY (for EXPENSE)
 * @property incomeSource Source of income (required for INCOME)
 * @property occurredAt Client epoch timestamp (milliseconds) when transaction occurred
 * @property original Original foreign currency details if transaction was entered in non-LKR
 * @property note Optional memo or merchant note
 * @property recurringId Optional ID of generating recurring template
 * @property expectedAmountLkrMinor Optional original expected amount for updated entries
 * @property invoiceId Optional ID of settled invoice
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 * @property deletedAt Soft deletion timestamp (milliseconds)
 */
data class Transaction(
    val id: String,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.ACTUAL,
    val amountLkrMinor: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val spendKind: SpendKind? = null,
    val incomeSource: IncomeSource? = null,
    val occurredAt: Long,
    val original: OriginalAmount? = null,
    val note: String? = null,
    val recurringId: String? = null,
    val expectedAmountLkrMinor: Long? = null,
    val invoiceId: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
) {
    /**
     * Validates transaction invariants:
     * - [id] and [accountId] must not be blank.
     * - EXPENSE requires [categoryId] (non-blank), [spendKind] (non-null), and no [toAccountId] or [incomeSource].
     * - TRANSFER requires [toAccountId] (non-blank and != [accountId]), [status] == ACTUAL, and no [categoryId], [incomeSource], or [spendKind].
     * - INCOME requires [incomeSource] and no [categoryId], [toAccountId], or [spendKind].
     * - [amountLkrMinor] > 0 except for CRYPTO income, which may be negative (realised loss) or positive (profit), but non-zero.
     * - If [original] is present: currency non-blank, fxRateMicros > 0, and amountMinor non-zero (>0 unless CRYPTO income).
     */
    fun validate() {
        require(id.isNotBlank()) { "Transaction ID must not be blank" }
        require(accountId.isNotBlank()) { "Account ID must not be blank" }

        when (type) {
            TransactionType.EXPENSE -> {
                require(!categoryId.isNullOrBlank()) { "Expense requires categoryId" }
                require(spendKind != null) { "Expense requires spendKind" }
                require(toAccountId == null) { "Expense cannot have toAccountId" }
                require(incomeSource == null) { "Expense cannot have incomeSource" }
            }
            TransactionType.TRANSFER -> {
                require(!toAccountId.isNullOrBlank()) { "Transfer requires toAccountId" }
                require(toAccountId != accountId) { "Transfer source and destination accounts must differ" }
                require(status == TransactionStatus.ACTUAL) { "Transfer status must be ACTUAL" }
                require(categoryId == null) { "Transfer cannot have categoryId" }
                require(incomeSource == null) { "Transfer cannot have incomeSource" }
                require(spendKind == null) { "Transfer cannot have spendKind" }
            }
            TransactionType.INCOME -> {
                require(incomeSource != null) { "Income requires incomeSource" }
                require(categoryId == null) { "Income cannot have categoryId" }
                require(toAccountId == null) { "Income cannot have toAccountId" }
                require(spendKind == null) { "Income cannot have spendKind" }
            }
        }

        val isCryptoIncome = type == TransactionType.INCOME && incomeSource == IncomeSource.CRYPTO
        if (isCryptoIncome) {
            require(amountLkrMinor != 0L) { "Crypto income amount cannot be zero" }
        } else {
            require(amountLkrMinor > 0) { "Amount must be positive" }
        }

        original?.let {
            require(it.currency.isNotBlank()) { "Original currency must not be blank" }
            require(it.fxRateMicros > 0) { "FX rate must be positive" }
            if (isCryptoIncome) {
                require(it.amountMinor != 0L) { "Original crypto amount cannot be zero" }
            } else {
                require(it.amountMinor > 0) { "Original amount must be positive" }
            }
        }
    }
}
