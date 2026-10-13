package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.CategoryDao
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.fakes.KavinduSampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val categoryDao: CategoryDao,
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
        category.validate()
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addCategory(uid, category)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun update(category: Category): Result<Unit> {
        category.validate()
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.updateCategory(uid, category)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
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
            val current = categoryDao.observeAll().first()
            if (current.isEmpty()) {
                for (cat in KavinduSampleData.defaultCategories) {
                    when (val res = remoteDataSource.addCategory(uid, cat)) {
                        is Result.Success -> {}
                        is Result.Error -> return res
                    }
                }
            }
        }
    }

    override fun suggestedFor(accountId: String): Flow<List<Category>> {
        return observeActive()
    }
}
