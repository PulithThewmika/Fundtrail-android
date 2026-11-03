package com.fundtrail.fakes

import com.fundtrail.domain.model.ReminderSettings
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.ReminderSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory stand-in for the Preferences DataStore backing of the reminder settings (#218). */
@Singleton
class FakeReminderSettingsRepository @Inject constructor() : ReminderSettingsRepository {
    private val settings = MutableStateFlow(ReminderSettings())

    override fun observe(): Flow<ReminderSettings> = settings

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> = runCatchingResult {
        settings.value = settings.value.copy(enabled = enabled)
    }

    override suspend fun setTime(minuteOfDay: Int): Result<Unit> = runCatchingResult {
        require(minuteOfDay in 0 until 24 * 60) { "minuteOfDay must be within a day" }
        settings.value = settings.value.copy(minuteOfDay = minuteOfDay)
    }
}
