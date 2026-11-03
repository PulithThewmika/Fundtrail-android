package com.fundtrail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Mirrors the `users/{uid}` profile document. Room holds only the signed-in user's row, so there's no uid column (architecture.md §4.4). */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val displayName: String,
    val incomeEstimateMinMinor: Long?,
    val incomeEstimateMaxMinor: Long?,
    val expectedSalaryMinor: Long?,
    val salaryPayday: Int,
    val schemaVersion: Int,
    val createdAt: Long?,
    val updatedAt: Long?,
)
