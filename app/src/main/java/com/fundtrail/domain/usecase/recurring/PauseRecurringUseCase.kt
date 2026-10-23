package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Pauses or resumes a template (#205). Pause is the dedicated repository call; resume is a plain update with
 * `isPaused = false`, so both survive a restart (they go through Firestore then the Room mirror, ADR-0004).
 */
class PauseRecurringUseCase @Inject constructor(private val recurring: RecurringRepository) {
    suspend fun pause(id: String): Result<Unit> = recurring.pause(id)

    suspend fun resume(template: RecurringTemplate): Result<Unit> = recurring.update(template.copy(isPaused = false))
}
