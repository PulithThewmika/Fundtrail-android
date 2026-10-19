package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.repository.CategoryRepository
import javax.inject.Inject

/** Categories the user can still pick; archived ones are left out (FR-21). */
class ObserveActiveCategoriesUseCase @Inject constructor(private val categories: CategoryRepository) {
    operator fun invoke() = categories.observeActive()
}
