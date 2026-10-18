package com.fundtrail.data.repository

import android.util.Log
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
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
            // No timeout: after sign-out Room is empty until the first sync lands, which can take longer than
            // any fixed wait. Seeding earlier would overwrite the user's real categories with the defaults.
            // Offline, this simply waits for the first server snapshot (DefaultDataSeeder runs in the background).
            syncStateDao.observe().first { it?.initialSyncDone == true }
            // The user may have signed out while this waited; never seed into another session.
            if (auth.currentUserId.first() != uid) return@runCatchingResult
            if (categoryDao.observeAll().first().isNotEmpty()) return@runCatchingResult
            for (cat in DefaultCategories.all) {
                // Only the sync listener writes to Room (architecture.md section 4.1). A failed write is logged and
                // Room stays empty, so the next launch seeds again instead of keeping a row Firestore lacks.
                val res = remoteDataSource.addCategory(uid, cat)
                if (res is Result.Error) Log.e(TAG, "Seeding ${cat.id} failed", res.exception)
            }
        }
    }

    override fun suggestedFor(accountId: String): Flow<List<Category>> = flow {
        // Window start is taken when the flow is collected, i.e. each time quick-add opens (FR-33).
        val since = System.currentTimeMillis() - USAGE_WINDOW_MILLIS
        emitAll(
            combine(
                categoryDao.observeAll(),
                transactionDao.observeMostRecentCategoryId(accountId),
                transactionDao.observeCategoryUsage(since),
            ) { categories, recentCategoryId, usage ->
                CategorySuggestions.rank(
                    categories = categories.map { it.toDomain() },
                    recentForAccountId = recentCategoryId,
                    usesByCategory = usage.associate { it.categoryId to it.uses },
                )
            },
        )
    }
    // TODO(M3): also rank by matching note or merchant text and remember corrections (FR-23, FR-24)

    private companion object {
        const val TAG = "CategoryRepository"
        val USAGE_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(30)
    }
}
