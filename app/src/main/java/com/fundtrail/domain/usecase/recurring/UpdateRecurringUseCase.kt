package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Saves edits to an existing template (#205). The repository update deliberately leaves out `nextDueDate`, so
 * editing the day of month or amount cannot rewind the cursor and make a paid month due again (schema §3.6).
 */
class UpdateRecurringUseCase @Inject constructor(private val recurring: RecurringRepository) {
    suspend operator fun invoke(template: RecurringTemplate): Result<Unit> = recurring.update(template)
}
