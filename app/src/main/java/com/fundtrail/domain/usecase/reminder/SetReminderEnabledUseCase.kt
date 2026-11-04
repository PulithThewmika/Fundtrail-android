package com.fundtrail.domain.usecase.reminder

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.ReminderScheduler
import com.fundtrail.domain.repository.ReminderSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Turns the daily reminder on or off (#219), keeping the stored flag and the scheduled work in step: enabling
 * schedules the next occurrence, disabling cancels it. The UI only calls this once the notification permission
 * is in hand, so a denied permission never reaches here as "enabled".
 */
class SetReminderEnabledUseCase @Inject constructor(
    private val settings: ReminderSettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        val result = settings.setEnabled(enabled)
        if (result is Result.Success) {
            if (enabled) scheduler.schedule(settings.observe().first().minuteOfDay) else scheduler.cancel()
        }
        return result
    }
}
