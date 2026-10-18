package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.DuplicateCategoryNameException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a category, or edits the name and committed/discretionary type of an existing one (FR-21, FR-22).
 *
 * A name must not be blank and must not match another active category, ignoring case. An archived category
 * does not block its name, and a category may keep its own name. A clash fails with [DuplicateCategoryNameException].
 *
 * Changing the type only changes what *new* entries copy from the category. Entries already saved keep
 * the `spendKind` they were stored with, so past months never move between committed and discretionary.
 */
class SaveCategoryUseCase @Inject constructor(private val categories: CategoryRepository) {

    /** [id] null creates a new category; otherwise the existing one is edited. */
    suspend operator fun invoke(id: String?, name: String, kind: SpendKind, icon: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Error(IllegalArgumentException("Category name must not be blank"))
        val all = categories.observeAll().first()
        if (all.any { !it.archived && it.id != id && it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.Error(DuplicateCategoryNameException())
        }
        if (id == null) {
            val sortOrder = (all.maxOfOrNull { it.sortOrder } ?: 0) + 1
            return categories.add(
                Category(id = "cat_${UUID.randomUUID()}", name = trimmed, defaultSpendKind = kind, icon = icon, sortOrder = sortOrder),
            )
        }
        val existing = all.firstOrNull { it.id == id }
            ?: return Result.Error(IllegalArgumentException("Category not found: $id"))
        return categories.update(existing.copy(name = trimmed, defaultSpendKind = kind, icon = icon))
    }
}
