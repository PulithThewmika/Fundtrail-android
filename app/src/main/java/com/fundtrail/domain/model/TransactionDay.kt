package com.fundtrail.domain.model

import java.time.LocalDate

/**
 * One day's rows for the history list (FR-100), newest first within the day.
 *
 * @property netMinor Actual income minus actual expense for the day; transfers never count (FR-61), and a
 * crypto loss is already negative in [Transaction.amountLkrMinor], so it subtracts here too.
 */
data class TransactionDay(
    val date: LocalDate,
    val transactions: List<Transaction>,
    val netMinor: Long,
)
