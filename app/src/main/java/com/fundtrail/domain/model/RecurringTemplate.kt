package com.fundtrail.domain.model

import java.time.LocalDate

/** FR-65: how often a recurring template comes due. */
enum class RecurringCadence { WEEKLY, MONTHLY, YEARLY }

/**
 * Domain model representing a recurring expense or income template.
 *
 * A due item is derived - [isDue] - and nothing is stored for it.
 *
 * @property id Unique template ID
 * @property name User-facing name, e.g. "Rent"
 * @property categoryId Associated expense category ID
 * @property accountId Account the transaction posts to
 * @property expectedAmountMinor Expected amount in LKR minor units
 * @property isVariableAmount Whether the actual amount can differ from [expectedAmountMinor] on confirm
 * @property cadence How often this recurs (FR-65); defaults to monthly for templates created before this field existed
 * @property dayOfMonth The due day: 1-28 for [RecurringCadence.MONTHLY]/[RecurringCadence.YEARLY], 1-7 (Monday-Sunday) for [RecurringCadence.WEEKLY]
 * @property nextDueDate The next date this is due; advanced by [cadence]'s period on confirm/skip
 * @property isPaused Paused templates never come due
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 * @property deletedAt Soft-delete tombstone (milliseconds); the sync mirror removes such templates from Room
 */
data class RecurringTemplate(
    val id: String,
    val name: String,
    val categoryId: String,
    val accountId: String,
    val expectedAmountMinor: Long,
    val isVariableAmount: Boolean = false,
    val cadence: RecurringCadence = RecurringCadence.MONTHLY,
    val dayOfMonth: Int,
    val nextDueDate: LocalDate,
    val isPaused: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
) {
    /** Derived, never stored: not paused and [nextDueDate] has arrived. */
    fun isDue(today: LocalDate): Boolean = !isPaused && !nextDueDate.isAfter(today)

    /**
     * The template with [nextDueDate] advanced by one [cadence] period, same [dayOfMonth].
     * Used by `confirm`/`skip`, which write this as part of the same atomic batch
     * as the confirmed transaction (confirm only).
     */
    fun advanced(): RecurringTemplate = copy(
        nextDueDate = when (cadence) {
            RecurringCadence.WEEKLY -> nextDueDate.plusWeeks(1)
            RecurringCadence.MONTHLY -> nextDueDate.plusMonths(1)
            RecurringCadence.YEARLY -> nextDueDate.plusYears(1)
        },
    )

    /**
     * Validates template invariants:
     * - [id], [name], [categoryId], [accountId] must not be blank.
     * - [expectedAmountMinor] must be > 0.
     * - [dayOfMonth] must be between 1 and 28, or 1 and 7 for a weekly cadence.
     */
    fun validate() {
        require(id.isNotBlank()) { "Recurring template ID must not be blank" }
        require(name.isNotBlank()) { "Recurring template name must not be blank" }
        require(categoryId.isNotBlank()) { "Recurring template category must not be blank" }
        require(accountId.isNotBlank()) { "Recurring template account must not be blank" }
        require(expectedAmountMinor > 0) { "Recurring template amount must be greater than zero" }
        val validRange = if (cadence == RecurringCadence.WEEKLY) 1..7 else 1..28
        require(dayOfMonth in validRange) { "Recurring template day must be in $validRange" }
    }
}
