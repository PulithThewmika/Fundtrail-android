package com.fundtrail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fundtrail.domain.model.AccountKind

/** Mirrors `accounts/{id}`. */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: AccountKind,
    val currency: String,
    val openingBalanceLkrMinor: Long,
    val openingBalanceAt: Long,
    val archived: Boolean,
    val sortOrder: Int,
    val createdAt: Long?,
    val updatedAt: Long?,
)
