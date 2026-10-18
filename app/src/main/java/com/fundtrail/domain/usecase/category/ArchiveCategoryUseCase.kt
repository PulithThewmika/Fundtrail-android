package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.CategoryRepository
import javax.inject.Inject

/** Hides a category from new entries but keeps it, so past spending still shows its name (FR-21). */
class ArchiveCategoryUseCase @Inject constructor(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = categories.archive(id)
}
