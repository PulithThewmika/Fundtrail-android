package com.fundtrail.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.fundtrail.domain.model.ReminderSettings
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.ReminderSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReminderSettingsRepositoryImpl @Inject constructor(
    private val preferences: DataStore<Preferences>,
) : ReminderSettingsRepository {

    override fun observe(): Flow<ReminderSettings> = preferences.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[ENABLED] ?: false,
            minuteOfDay = (prefs[MINUTE] ?: ReminderSettings.DEFAULT_MINUTE_OF_DAY).coerceIn(0, 24 * 60 - 1),
        )
    }

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> = runCatchingResult {
        preferences.edit { it[ENABLED] = enabled }
    }

    override suspend fun setTime(minuteOfDay: Int): Result<Unit> = runCatchingResult {
        require(minuteOfDay in 0 until 24 * 60) { "minuteOfDay must be within a day" }
        preferences.edit { it[MINUTE] = minuteOfDay }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val MINUTE = intPreferencesKey("daily_reminder_minute_of_day")
    }
}
