package com.fundtrail.fakes

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeCategoryRepository @Inject constructor() : CategoryRepository {

    private val categoriesFlow = MutableStateFlow<List<Category>>(KavinduSampleData.defaultCategories)

    override fun observeActive(): Flow<List<Category>> {
        return categoriesFlow.map { list -> list.filter { !it.isArchived } }
    }

    override fun observeAll(): Flow<List<Category>> {
        return categoriesFlow
    }

    override suspend fun add(category: Category): Result<Unit> {
        return try {
            category.validate()
            categoriesFlow.value = categoriesFlow.value + category
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Error(e)
        }
    }

    override suspend fun update(category: Category): Result<Unit> {
        return try {
            category.validate()
            val currentList = categoriesFlow.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == category.id }
            if (index != -1) {
                currentList[index] = category
                categoriesFlow.value = currentList
                Result.Success(Unit)
            } else {
                Result.Error(IllegalArgumentException("Category not found: ${category.id}"))
            }
        } catch (e: Throwable) {
            Result.Error(e)
        }
    }

    override suspend fun archive(id: String): Result<Unit> {
        val currentList = categoriesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        return if (index != -1) {
            currentList[index] = currentList[index].copy(isArchived = true)
            categoriesFlow.value = currentList
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("Category not found: $id"))
        }
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> {
        if (categoriesFlow.value.isEmpty()) {
            categoriesFlow.value = KavinduSampleData.defaultCategories
        }
        return Result.Success(Unit)
    }

    override fun suggestedFor(accountId: String): Flow<List<Category>> {
        return observeActive()
    }
}
