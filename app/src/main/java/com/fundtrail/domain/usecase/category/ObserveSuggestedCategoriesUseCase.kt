package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.repository.CategoryRepository
import javax.inject.Inject

/** Active categories ranked for a new expense on an account; the first is the pre-selected chip (FR-23, FR-33). */
class ObserveSuggestedCategoriesUseCase @Inject constructor(private val categories: CategoryRepository) {
    operator fun invoke(accountId: String) = categories.suggestedFor(accountId)
}
