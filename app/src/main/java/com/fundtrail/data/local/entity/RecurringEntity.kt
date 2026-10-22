package com.fundtrail.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors `recurring/{id}`. Soft-deleted documents are removed, not stored. */
@Entity(
    tableName = "recurring",
    indices = [Index("nextDueDate")],
)
data class RecurringEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val accountId: String,
    val expectedAmountMinor: Long,
    val isVariableAmount: Boolean,
    val dayOfMonth: Int,
    val nextDueDate: String,
    val isPaused: Boolean,
    val createdAt: Long?,
    val updatedAt: Long?,
)
