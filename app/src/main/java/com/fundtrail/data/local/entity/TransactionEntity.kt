package com.fundtrail.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType

/** Mirrors `transactions/{id}`. Soft-deleted documents are removed, not stored (architecture.md §4.3). */
@Entity(
    tableName = "transactions",
    indices = [Index("occurredAt"), Index("accountId"), Index("categoryId")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val status: TransactionStatus,
    val amountLkrMinor: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val spendKind: SpendKind?,
    val incomeSource: IncomeSource?,
    val occurredAt: Long,
    @Embedded(prefix = "original_") val original: OriginalAmount?,
    val note: String?,
    val recurringId: String?,
    val expectedAmountLkrMinor: Long?,
    val invoiceId: String?,
    val createdAt: Long?,
    val updatedAt: Long?,
)
