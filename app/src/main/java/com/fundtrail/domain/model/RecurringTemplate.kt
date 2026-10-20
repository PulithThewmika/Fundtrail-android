package com.fundtrail.domain.model

import java.time.LocalDate

/**
 * Domain model representing a recurring monthly expense or income template.
 *
 * A due item is derived - [isDue] - and nothing is stored for it. Monthly frequency only in M3.
 *
 * @property id Unique template ID
 * @property name User-facing name, e.g. "Rent"
 * @property categoryId Associated expense category ID
 * @property accountId Account the transaction posts to
 * @property expectedAmountMinor Expected amount in LKR minor units
 * @property isVariableAmount Whether the actual amount can differ from [expectedAmountMinor] on confirm
 * @property dayOfMonth Day of month this recurs on, 1-28
 * @property nextDueDate The next date this is due; advanced by one month on confirm/skip
 * @property isPaused Paused templates never come due
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 */
data class RecurringTemplate(
    val id: String,
    val name: String,
    val categoryId: String,
    val accountId: String,
    val expectedAmountMinor: Long,
    val isVariableAmount: Boolean = false,
    val dayOfMonth: Int,
    val nextDueDate: LocalDate,
    val isPaused: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    /** Derived, never stored: not paused and [nextDueDate] has arrived. */
    fun isDue(today: LocalDate): Boolean = !isPaused && !nextDueDate.isAfter(today)

    /**
     * The template with [nextDueDate] advanced by one month, same [dayOfMonth].
     * Used by `confirm`/`skip`, which write this as part of the same atomic batch
     * as the confirmed transaction (confirm only).
     */
    fun advanced(): RecurringTemplate = copy(nextDueDate = nextDueDate.plusMonths(1))

    /**
     * Validates template invariants:
     * - [id], [name], [categoryId], [accountId] must not be blank.
     * - [expectedAmountMinor] must be > 0.
     * - [dayOfMonth] must be between 1 and 28.
     */
    fun validate() {
        require(id.isNotBlank()) { "Recurring template ID must not be blank" }
        require(name.isNotBlank()) { "Recurring template name must not be blank" }
        require(categoryId.isNotBlank()) { "Recurring template category must not be blank" }
        require(accountId.isNotBlank()) { "Recurring template account must not be blank" }
        require(expectedAmountMinor > 0) { "Recurring template amount must be greater than zero" }
        require(dayOfMonth in 1..28) { "Recurring template day of month must be between 1 and 28" }
    }
}
