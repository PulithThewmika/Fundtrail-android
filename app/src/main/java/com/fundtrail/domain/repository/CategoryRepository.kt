package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing expense categories.
 */
interface CategoryRepository {
    fun observeActive(): Flow<List<Category>>
    fun observeAll(): Flow<List<Category>>
    suspend fun add(category: Category): Result<Unit>
    suspend fun update(category: Category): Result<Unit>
    suspend fun archive(id: String): Result<Unit>
    suspend fun seedDefaultsIfEmpty(): Result<Unit>
    fun suggestedFor(accountId: String): Flow<List<Category>>
}
