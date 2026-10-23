package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.RecurringRepository
import javax.inject.Inject

/** Skips the due item for [templateId] (#200, #202): advances the cursor by one month, writing no expense. */
class SkipRecurringUseCase @Inject constructor(private val recurring: RecurringRepository) {
    suspend operator fun invoke(templateId: String): Result<Unit> = recurring.skip(templateId)
}
