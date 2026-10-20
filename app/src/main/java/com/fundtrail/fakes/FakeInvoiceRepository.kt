package com.fundtrail.fakes

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeInvoiceRepository @Inject constructor() : InvoiceRepository {

    private val invoicesFlow = MutableStateFlow<List<Invoice>>(KavinduSampleData.defaultInvoices)
    val recordedTransactions = mutableListOf<Transaction>()

    override fun observeAll(): Flow<List<Invoice>> = invoicesFlow

    override suspend fun add(invoice: Invoice): Result<Unit> = runCatchingResult {
        invoice.validate()
        require(invoicesFlow.value.none { it.id == invoice.id }) { "Invoice already exists: ${invoice.id}" }
        invoicesFlow.value = invoicesFlow.value + invoice
    }

    override suspend fun update(invoice: Invoice): Result<Unit> = runCatchingResult {
        invoice.validate()
        val currentList = invoicesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == invoice.id }
        require(index != -1) { "Invoice not found: ${invoice.id}" }
        currentList[index] = invoice
        invoicesFlow.value = currentList
    }

    override suspend fun delete(id: String): Result<Unit> = runCatchingResult {
        val currentList = invoicesFlow.value.toMutableList()
        require(currentList.removeAll { it.id == id }) { "Invoice not found: $id" }
        invoicesFlow.value = currentList
    }

    override suspend fun markReceived(id: String, transaction: Transaction): Result<Unit> = runCatchingResult {
        transaction.validate()
        val currentList = invoicesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        require(index != -1) { "Invoice not found: $id" }
        // Both writes happen together, or neither does (one atomic batch).
        currentList[index] = currentList[index].copy(
            status = InvoiceStatus.RECEIVED,
            receivedTransactionId = transaction.id,
            receivedAt = transaction.occurredAt
        )
        recordedTransactions += transaction
        invoicesFlow.value = currentList
    }
}
