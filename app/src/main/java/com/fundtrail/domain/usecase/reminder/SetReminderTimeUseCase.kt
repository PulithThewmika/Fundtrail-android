package com.fundtrail.domain.usecase.reminder

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.ReminderScheduler
import com.fundtrail.domain.repository.ReminderSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Changes the reminder time (#219). When the reminder is on, it re-schedules so the new time replaces the old
 * one with no duplicate (#218 AC); when off, it only stores the preference.
 */
class SetReminderTimeUseCase @Inject constructor(
    private val settings: ReminderSettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(minuteOfDay: Int): Result<Unit> {
        val result = settings.setTime(minuteOfDay)
        if (result is Result.Success && settings.observe().first().enabled) scheduler.schedule(minuteOfDay)
        return result
    }
}
