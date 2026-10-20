package com.fundtrail.domain.model

import java.time.LocalDate

/**
 * Domain model representing a savings goal.
 *
 * Progress is the savings account balance minus [baselineSavedMinor] (the balance when the
 * goal started) - withdrawing from savings lowers progress instead of hiding it.
 *
 * @property id Unique goal ID
 * @property name User-facing goal name, e.g. "MacBook Pro M4"
 * @property targetLkrMinor Target amount in LKR minor units
 * @property deadline Date the goal should be reached by
 * @property baselineSavedMinor Amount already saved toward this goal when it was created, LKR minor units
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 * @property isActive Only one active goal per user
 */
data class Goal(
    val id: String,
    val name: String,
    val targetLkrMinor: Long,
    val deadline: LocalDate,
    val baselineSavedMinor: Long,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isActive: Boolean = true
) {
    /**
     * Validates goal invariants:
     * - [id] and [name] must not be blank.
     * - [targetLkrMinor] must be > 0.
     * - [baselineSavedMinor] must be >= 0.
     * - [deadline] must be after the creation date (today, when [createdAt] is unset).
     */
    fun validate(today: LocalDate = LocalDate.now()) {
        require(id.isNotBlank()) { "Goal ID must not be blank" }
        require(name.isNotBlank()) { "Goal name must not be blank" }
        require(targetLkrMinor > 0) { "Goal target must be greater than zero" }
        require(baselineSavedMinor >= 0) { "Goal baseline saved must not be negative" }
        require(deadline.isAfter(today)) { "Goal deadline must be after the creation date" }
    }
}
