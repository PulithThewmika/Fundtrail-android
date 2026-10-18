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
    /**
     * Active categories ranked for a new expense on [accountId]; the first is the pre-selected suggestion.
     * Fallback order: most recent category used with the account, then most-used overall, then `sortOrder`.
     * See [com.fundtrail.domain.model.CategorySuggestions].
     */
    fun suggestedFor(accountId: String): Flow<List<Category>>
}
