package com.fundtrail.domain.usecase.category

import com.fundtrail.domain.repository.CategoryRepository
import javax.inject.Inject

/** Every category, archived ones included, so past spending still shows its category name. */
class ObserveCategoriesUseCase @Inject constructor(private val categories: CategoryRepository) {
    operator fun invoke() = categories.observeAll()
}
