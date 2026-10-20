package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.InvoiceDao
import com.fundtrail.data.remote.source.InvoiceRemoteDataSource
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InvoiceRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val invoiceDao: InvoiceDao,
    private val remoteDataSource: InvoiceRemoteDataSource,
) : InvoiceRepository {

    override fun observeAll(): Flow<List<Invoice>> {
        return invoiceDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun add(invoice: Invoice): Result<Unit> {
        val validationResult = runCatchingResult { invoice.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addInvoice(uid, invoice)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun update(invoice: Invoice): Result<Unit> {
        val validationResult = runCatchingResult { invoice.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.updateInvoice(uid, invoice)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.deleteInvoice(uid, id)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun markReceived(id: String, transaction: Transaction): Result<Unit> {
        val validationResult = runCatchingResult {
            transaction.validate()
            require(transaction.type == TransactionType.INCOME) { "Settling transaction must be INCOME" }
            require(transaction.status == TransactionStatus.ACTUAL) { "Settling transaction must be ACTUAL" }
        }
        if (validationResult is Result.Error) return validationResult

        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))

        val invoiceEntity = invoiceDao.getById(id)
            ?: return Result.Error(IllegalArgumentException("Invoice not found: $id"))

        if (invoiceEntity.status == InvoiceStatus.RECEIVED) {
            return Result.Error(IllegalStateException("Invoice already received: $id"))
        }

        val noteParts = listOfNotNull(
            invoiceEntity.client,
            invoiceEntity.project,
            invoiceEntity.milestone.takeUnless { it.isNullOrBlank() }
        )
        val note = noteParts.joinToString(" - ")

        val settlingTx = transaction.copy(
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            incomeSource = IncomeSource.FREELANCE,
            note = note,
            invoiceId = id
        )

        return remoteDataSource.markReceived(uid, id, settlingTx)
    }
}
