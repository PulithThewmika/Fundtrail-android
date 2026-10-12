package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Domain repository interface for managing financial transactions.
 */
interface TransactionRepository {
    fun observeMonth(yearMonth: YearMonth): Flow<List<Transaction>>
    fun observeById(id: String): Flow<Transaction?>
    suspend fun add(transaction: Transaction): Result<Unit>
    suspend fun update(transaction: Transaction): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
