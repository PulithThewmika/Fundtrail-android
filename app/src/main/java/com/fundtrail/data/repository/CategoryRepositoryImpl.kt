package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.CategoryDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val categoryDao: CategoryDao,
    private val syncStateDao: SyncStateDao,
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
            syncStateDao.observe().first { it?.initialSyncDone == true }
            val current = categoryDao.observeAll().first()
            if (current.isEmpty()) {
                for (cat in defaultCategories) {
                    when (val res = remoteDataSource.addCategory(uid, cat)) {
                        is Result.Success -> {}
                        is Result.Error -> return res
                    }
                }
            }
        }
    }

    override fun suggestedFor(accountId: String): Flow<List<Category>> {
        // TODO(M3): rank by last-used category and matching note/merchant text (FR-23, FR-24)
        return observeActive()
    }

    private companion object {
        val defaultCategories = listOf(
            // Committed
            Category(id = "cat_rent", name = "Rent", defaultSpendKind = SpendKind.COMMITTED, icon = "home", sortOrder = 1),
            Category(id = "cat_utilities", name = "Utilities", defaultSpendKind = SpendKind.COMMITTED, icon = "bolt", sortOrder = 2),
            Category(id = "cat_phone_internet", name = "Phone and internet", defaultSpendKind = SpendKind.COMMITTED, icon = "phone", sortOrder = 3),
            Category(id = "cat_gym", name = "Gym", defaultSpendKind = SpendKind.COMMITTED, icon = "fitness_center", sortOrder = 4),
            Category(id = "cat_subscriptions", name = "Subscriptions", defaultSpendKind = SpendKind.COMMITTED, icon = "subscriptions", sortOrder = 5),
            Category(id = "cat_groceries", name = "Groceries", defaultSpendKind = SpendKind.COMMITTED, icon = "shopping_cart", sortOrder = 6),
            Category(id = "cat_health", name = "Health", defaultSpendKind = SpendKind.COMMITTED, icon = "local_hospital", sortOrder = 7),
            // Discretionary
            Category(id = "cat_ride_hailing", name = "Ride-hailing", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "directions_car", sortOrder = 8),
            Category(id = "cat_food_delivery", name = "Food delivery", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "fastfood", sortOrder = 9),
            Category(id = "cat_coffee_dining", name = "Coffee and dining", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "coffee", sortOrder = 10),
            Category(id = "cat_shopping", name = "Shopping", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "shopping_bag", sortOrder = 11),
            Category(id = "cat_entertainment", name = "Entertainment", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "movie", sortOrder = 12),
            Category(id = "cat_travel", name = "Travel", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "flight", sortOrder = 13),
            Category(id = "cat_other", name = "Other", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "category", sortOrder = 14),
            // Fallback
            Category(id = "cat_uncategorised", name = "Uncategorised", defaultSpendKind = SpendKind.DISCRETIONARY, icon = "help_outline", sortOrder = 15)
        )
    }
}
