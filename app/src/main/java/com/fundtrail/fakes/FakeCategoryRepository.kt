package com.fundtrail.fakes

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
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
        return categoriesFlow.map { list -> list.filter { !it.archived } }
    }

    override fun observeAll(): Flow<List<Category>> {
        return categoriesFlow
    }

    override suspend fun add(category: Category): Result<Unit> = runCatchingResult {
        category.validate()
        require(categoriesFlow.value.none { it.id == category.id }) { "Category already exists: ${category.id}" }
        categoriesFlow.value = categoriesFlow.value + category
    }

    override suspend fun update(category: Category): Result<Unit> = runCatchingResult {
        category.validate()
        val currentList = categoriesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == category.id }
        require(index != -1) { "Category not found: ${category.id}" }
        currentList[index] = category
        categoriesFlow.value = currentList
    }

    override suspend fun archive(id: String): Result<Unit> = runCatchingResult {
        val currentList = categoriesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        require(index != -1) { "Category not found: $id" }
        currentList[index] = currentList[index].copy(archived = true)
        categoriesFlow.value = currentList
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> = runCatchingResult {
        if (categoriesFlow.value.isEmpty()) {
            categoriesFlow.value = KavinduSampleData.defaultCategories
        }
    }

    override fun suggestedFor(accountId: String): Flow<List<Category>> {
        return observeActive()
    }
}
