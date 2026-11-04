package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.RecurringRepository
import javax.inject.Inject

/** Soft-deletes a template (#205); the sync mirror then drops it from Room. Past entries are kept. */
class DeleteRecurringUseCase @Inject constructor(private val recurring: RecurringRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = recurring.delete(id)
}
