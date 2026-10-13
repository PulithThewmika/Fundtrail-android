package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.TransactionDao
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth
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
        transaction.validate()
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addTransaction(uid, transaction)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun update(transaction: Transaction): Result<Unit> {
        transaction.validate()
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.updateTransaction(uid, transaction)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
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
