package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Domain repository interface for freelance invoices. */
interface InvoiceRepository {
    fun observeAll(): Flow<List<Invoice>>
    suspend fun add(invoice: Invoice): Result<Unit>
    suspend fun update(invoice: Invoice): Result<Unit>
    suspend fun delete(id: String): Result<Unit>

    /**
     * Marks [id] as RECEIVED and records [transaction] as the settling INCOME transaction.
     * One atomic batch: the invoice update and the income transaction write together, or not at all.
     */
    suspend fun markReceived(id: String, transaction: Transaction): Result<Unit>
}
