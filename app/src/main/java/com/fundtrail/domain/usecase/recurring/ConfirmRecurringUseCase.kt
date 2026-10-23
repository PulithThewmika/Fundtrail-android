package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Confirms the due item for [templateId] (#200, #202): writes one ACTUAL committed expense and advances the
 * template's cursor, atomically, through [RecurringRepository.confirm]. [actualAmountMinor] is the expected
 * amount for a fixed template and the edited amount for a variable one; the repository rejects a fixed template
 * confirmed with a different amount. [occurredAt] is the due date, so the expense lands in the month it settles.
 */
class ConfirmRecurringUseCase @Inject constructor(private val recurring: RecurringRepository) {
    suspend operator fun invoke(templateId: String, actualAmountMinor: Long, occurredAt: Long): Result<Unit> =
        recurring.confirm(templateId, actualAmountMinor, occurredAt)
}
