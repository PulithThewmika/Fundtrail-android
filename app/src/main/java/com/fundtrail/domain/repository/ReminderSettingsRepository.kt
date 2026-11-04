package com.fundtrail.domain.repository

import com.fundtrail.domain.model.ReminderSettings
import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/** Per-device daily-reminder settings (#219), backed by Preferences DataStore. */
interface ReminderSettingsRepository {
    fun observe(): Flow<ReminderSettings>
    suspend fun setEnabled(enabled: Boolean): Result<Unit>
    suspend fun setTime(minuteOfDay: Int): Result<Unit>
}
