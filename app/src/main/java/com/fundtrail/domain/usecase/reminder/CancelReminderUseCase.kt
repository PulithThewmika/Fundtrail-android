package com.fundtrail.domain.usecase.reminder

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.ReminderScheduler
import com.fundtrail.domain.repository.ReminderSettingsRepository
import javax.inject.Inject

/** Logout path (#218): cancel the scheduled work and reset the toggle so the next user starts clean. */
class CancelReminderUseCase @Inject constructor(
    private val settings: ReminderSettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(): Result<Unit> {
        // The caller is already signed out, so a scheduler that throws must come back as an error rather than
        // propagate and leave the UI mid-sign-out.
        val cancelled = runCatchingResult { scheduler.cancel() }
        val reset = settings.setEnabled(false)
        return if (cancelled is Result.Error) cancelled else reset
    }
}
