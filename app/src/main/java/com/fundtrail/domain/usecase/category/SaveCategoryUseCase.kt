package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a category, or edits the name and committed/discretionary type of an existing one (FR-21, FR-22).
 *
 * Changing the type only changes what *new* entries copy from the category. Entries already saved keep
 * the `spendKind` they were stored with, so past months never move between committed and discretionary.
 */
class SaveCategoryUseCase @Inject constructor(private val categories: CategoryRepository) {

    /** [id] null creates a new category; otherwise the existing one is edited. */
    suspend operator fun invoke(id: String?, name: String, kind: SpendKind, icon: String): Result<Unit> {
        val trimmed = name.trim()
        if (id == null) {
            val sortOrder = (categories.observeAll().first().maxOfOrNull { it.sortOrder } ?: 0) + 1
            return categories.add(
                Category(id = "cat_${UUID.randomUUID()}", name = trimmed, defaultSpendKind = kind, icon = icon, sortOrder = sortOrder),
            )
        }
        val existing = categories.observeAll().first().firstOrNull { it.id == id }
            ?: return Result.Error(IllegalArgumentException("Category not found: $id"))
        return categories.update(existing.copy(name = trimmed, defaultSpendKind = kind, icon = icon))
    }
}
