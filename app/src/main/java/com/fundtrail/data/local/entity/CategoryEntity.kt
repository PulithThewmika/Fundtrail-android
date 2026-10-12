package com.fundtrail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fundtrail.domain.model.SpendKind

/** Mirrors `categories/{id}`. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultSpendKind: SpendKind,
    val icon: String,
    val archived: Boolean,
    val sortOrder: Int,
    val createdAt: Long?,
    val updatedAt: Long?,
)
