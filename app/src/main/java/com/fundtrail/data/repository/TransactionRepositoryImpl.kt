package com.fundtrail.data.repository

import com.google.firebase.Timestamp
import com.fundtrail.data.local.dao.TransactionDao
import com.fundtrail.data.remote.dto.toDto
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.util.Date
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val transactionDao: TransactionDao,
    private val remoteDataSource: TransactionRemoteDataSource,
) : TransactionRepository {

    override fun observeMonth(yearMonth: YearMonth): Flow<List<Transaction>> {
        val (start, end) = yearMonth.toMillisRange()
        return transactionDao.observeBetween(start, end).map { entities ->
            entities.map { it.toDomain() }
                .filter { it.deletedAt == null }
                .sortedByDescending { it.occurredAt }
        }
    }

    override fun observeById(id: String): Flow<Transaction?> {
        return transactionDao.observeById(id).map { entity ->
            entity?.toDomain()?.takeIf { it.deletedAt == null }
        }
    }

    override suspend fun add(transaction: Transaction): Result<Unit> {
        val validationResult = runCatchingResult { transaction.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addTransaction(uid, transaction)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun update(transaction: Transaction): Result<Unit> {
        val validationResult = runCatchingResult { transaction.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))

        val old = transactionDao.observeById(transaction.id).first()?.toDomain()
        val updates = mutableMapOf<String, Any?>()
        if (old == null || old.type != transaction.type) updates["type"] = transaction.type.name
        if (old == null || old.status != transaction.status) updates["status"] = transaction.status.name
        if (old == null || old.amountLkrMinor != transaction.amountLkrMinor) updates["amountLkrMinor"] = transaction.amountLkrMinor
        if (old == null || old.accountId != transaction.accountId) updates["accountId"] = transaction.accountId
        if (old == null || old.toAccountId != transaction.toAccountId) updates["toAccountId"] = transaction.toAccountId
        if (old == null || old.categoryId != transaction.categoryId) updates["categoryId"] = transaction.categoryId
        if (old == null || old.spendKind != transaction.spendKind) updates["spendKind"] = transaction.spendKind?.name
        if (old == null || old.incomeSource != transaction.incomeSource) updates["incomeSource"] = transaction.incomeSource?.name
        if (old == null || old.occurredAt != transaction.occurredAt) updates["occurredAt"] = Timestamp(Date(transaction.occurredAt))
        if (old == null || old.original != transaction.original) updates["original"] = transaction.original?.toDto()
        if (old == null || old.note != transaction.note) updates["note"] = transaction.note
        if (old == null || old.recurringId != transaction.recurringId) updates["recurringId"] = transaction.recurringId
        if (old == null || old.expectedAmountLkrMinor != transaction.expectedAmountLkrMinor) updates["expectedAmountLkrMinor"] = transaction.expectedAmountLkrMinor
        if (old == null || old.invoiceId != transaction.invoiceId) updates["invoiceId"] = transaction.invoiceId

        return if (updates.isNotEmpty()) {
            remoteDataSource.updateTransaction(uid, transaction.id, updates)
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.deleteTransaction(uid, id)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }
}
