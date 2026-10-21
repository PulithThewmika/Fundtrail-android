package com.fundtrail.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fundtrail.domain.model.InvoiceStatus

/** Mirrors `invoices/{id}`. Soft-deleted documents are removed, not stored. */
@Entity(
    tableName = "invoices",
    indices = [Index("status"), Index("dueDate")],
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val client: String,
    val project: String,
    val milestone: String?,
    val amountMinor: Long,
    val currency: String,
    val dueDate: String,
    val status: InvoiceStatus,
    val receivedTransactionId: String?,
    val receivedAt: Long?,
    val lastNotifiedAt: Long?,
    val note: String?,
    val createdAt: Long?,
    val updatedAt: Long?,
    val issuedAt: Long?,
)
