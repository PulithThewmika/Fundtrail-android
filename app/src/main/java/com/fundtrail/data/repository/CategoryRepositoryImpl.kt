package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.CategoryDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.local.dao.TransactionDao
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.CategorySuggestions
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val categoryDao: CategoryDao,
    private val syncStateDao: SyncStateDao,
    private val transactionDao: TransactionDao,
    private val remoteDataSource: CategoryRemoteDataSource,
) : CategoryRepository {

    override fun observeActive(): Flow<List<Category>> {
        return categoryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }.filter { !it.archived }
        }
    }

    override fun observeAll(): Flow<List<Category>> {
        return categoryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun add(category: Category): Result<Unit> {
        val validationResult = runCatchingResult { category.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addCategory(uid, category)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun update(category: Category): Result<Unit> {
        val validationResult = runCatchingResult { category.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))

        val old = categoryDao.observeAll().first().find { it.id == category.id }?.toDomain()
        val updates = mutableMapOf<String, Any?>()
        if (old == null || old.name != category.name) updates["name"] = category.name
        if (old == null || old.defaultSpendKind != category.defaultSpendKind) updates["defaultSpendKind"] = category.defaultSpendKind.name
        if (old == null || old.icon != category.icon) updates["icon"] = category.icon
        if (old == null || old.archived != category.archived) updates["archived"] = category.archived
        if (old == null || old.sortOrder != category.sortOrder) updates["sortOrder"] = category.sortOrder

        return if (updates.isNotEmpty()) {
            remoteDataSource.updateCategory(uid, category.id, updates)
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun archive(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.updateCategory(uid, id, mapOf("archived" to true))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return runCatchingResult {
            try {
                kotlinx.coroutines.withTimeout(2_000) {
                    syncStateDao.observe().first { it?.initialSyncDone == true }
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                // Proceed if initial sync takes too long or isn't connected
            }
            val current = categoryDao.observeAll().first()
            if (current.isEmpty()) {
                for (cat in DefaultCategories.all) {
                    when (remoteDataSource.addCategory(uid, cat)) {
                        is Result.Success -> {}
                        is Result.Error -> {
                            categoryDao.upsertAll(listOf(cat.toEntity()))
                        }
                    }
                }
            }
        }
    }

    override fun suggestedFor(accountId: String): Flow<List<Category>> = combine(
        categoryDao.observeAll(),
        transactionDao.observeMostRecentCategoryId(accountId),
        transactionDao.observeCategoryUsage(),
    ) { categories, recentCategoryId, usage ->
        CategorySuggestions.rank(
            categories = categories.map { it.toDomain() },
            recentForAccountId = recentCategoryId,
            usesByCategory = usage.associate { it.categoryId to it.uses },
        )
    }
    // TODO(M3): also rank by matching note or merchant text and remember corrections (FR-23, FR-24)
}
