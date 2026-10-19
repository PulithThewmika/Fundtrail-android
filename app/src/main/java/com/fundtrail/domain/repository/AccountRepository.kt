package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing financial accounts.
 */
interface AccountRepository {
    fun observeActive(): Flow<List<Account>>
    fun observeAll(): Flow<List<Account>>
    suspend fun add(account: Account): Result<Unit>
    suspend fun rename(id: String, newName: String): Result<Unit>
    suspend fun archive(id: String): Result<Unit>
    suspend fun seedDefaultsIfEmpty(): Result<Unit>
}
