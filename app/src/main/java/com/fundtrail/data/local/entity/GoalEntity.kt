package com.fundtrail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors `goals/{id}`. [deadlineEpochDay] stores [java.time.LocalDate.toEpochDay] so the
 * value is timezone-independent (schema.md §3.7).
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetLkrMinor: Long,
    val deadlineEpochDay: Long,
    val baselineSavedMinor: Long,
    val isActive: Boolean,
    val createdAt: Long?,
    val updatedAt: Long?,
)
