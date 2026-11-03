package com.fundtrail.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Domain model representing a savings goal.
 *
 * Progress is not tied to a single account's balance: it is [baselineSavedMinor] (what was
 * already saved when the goal started) plus whatever the goal projection use case counts as
 * actual pace since then (schema.md §6.3).
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
     * - [deadline] must be after the creation date ([createdAt], or [today] when unset - a new goal).
     */
    fun validate(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()) {
        require(id.isNotBlank()) { "Goal ID must not be blank" }
        require(name.isNotBlank()) { "Goal name must not be blank" }
        require(targetLkrMinor > 0) { "Goal target must be greater than zero" }
        require(baselineSavedMinor >= 0) { "Goal baseline saved must not be negative" }
        val createdOn = createdAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: today
        require(deadline.isAfter(createdOn)) { "Goal deadline must be after the creation date" }
    }
}
