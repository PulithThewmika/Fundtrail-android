package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.RecurringRepository
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a recurring template from the add form (#203, #205). The first [RecurringTemplate.nextDueDate] is the
 * next occurrence of [dayOfMonth] on or after today in [APP_ZONE]. [RecurringTemplate.validate] (run by the
 * repository) enforces the 1..28 day range and a positive amount, so a bad day or amount comes back as an error.
 */
class AddRecurringUseCase @Inject constructor(
    private val recurring: RecurringRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        name: String,
        categoryId: String,
        accountId: String,
        expectedAmountMinor: Long,
        isVariableAmount: Boolean,
        dayOfMonth: Int,
    ): Result<Unit> {
        val template = RecurringTemplate(
            id = "rec_${UUID.randomUUID()}",
            name = name.trim(),
            categoryId = categoryId,
            accountId = accountId,
            expectedAmountMinor = expectedAmountMinor,
            isVariableAmount = isVariableAmount,
            dayOfMonth = dayOfMonth,
            nextDueDate = nextDueDate(dayOfMonth),
        )
        return recurring.add(template)
    }

    private fun nextDueDate(dayOfMonth: Int): LocalDate {
        val today = LocalDate.now(clock.withZone(APP_ZONE))
        // dayOfMonth is 1..28, so withDayOfMonth never overflows a short month.
        val candidate = today.withDayOfMonth(dayOfMonth.coerceIn(1, 28))
        return if (candidate.isBefore(today)) candidate.plusMonths(1) else candidate
    }
}
