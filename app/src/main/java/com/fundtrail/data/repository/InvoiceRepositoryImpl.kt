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
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
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

    /**
     * The stamp goes to Firestore, not straight to Room. The sync mirror rebuilds every invoice row from the
     * remote document on the first snapshot of each run (ADR-0004), so a Room-only value is wiped on the next
     * app start and the same invoice is notified again a day later. The listener brings this value back down.
     */
    override suspend fun updateLastNotifiedAt(id: String, notifiedAt: Long): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return remoteDataSource.updateInvoice(uid, id, mapOf("lastNotifiedAt" to Timestamp(Date(notifiedAt))))
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

        // Reads the Room mirror, so it can miss an invoice that has not synced down yet.
        val invoiceEntity = invoiceDao.getById(id)
            ?: return Result.Error(IllegalArgumentException("Invoice not found locally (not synced yet?): $id"))

        // Local guard against a double tap only; see the markReceived TODO(M3) on the remote source.
        if (invoiceEntity.status == InvoiceStatus.RECEIVED) {
            return Result.Error(IllegalStateException("Invoice already received: $id"))
        }

        // A foreign-currency invoice settles with its original amount and locked rate (schema.md 3.5, ADR-0003).
        if (invoiceEntity.currency != LKR) {
            val original = transaction.original
            if (original == null || original.currency != invoiceEntity.currency) {
                return Result.Error(
                    IllegalArgumentException(
                        "Settling a ${invoiceEntity.currency} invoice needs an original ${invoiceEntity.currency} amount and rate"
                    )
                )
            }
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

    private companion object {
        const val LKR = "LKR"
    }
}
