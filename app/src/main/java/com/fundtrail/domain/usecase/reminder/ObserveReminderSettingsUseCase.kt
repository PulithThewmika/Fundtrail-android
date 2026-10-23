package com.fundtrail.domain.usecase.reminder

import com.fundtrail.domain.model.ReminderSettings
import com.fundtrail.domain.repository.ReminderSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The per-device daily-reminder settings for the More -> Reminders screen (#219). */
class ObserveReminderSettingsUseCase @Inject constructor(private val settings: ReminderSettingsRepository) {
    operator fun invoke(): Flow<ReminderSettings> = settings.observe()
}
