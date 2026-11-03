package com.fundtrail.fakes

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSettingsRepository @Inject constructor() : SettingsRepository {

    private val settingsFlow = MutableStateFlow(KavinduSampleData.defaultSettings)

    override fun observe(): Flow<UserSettings> = settingsFlow

    override suspend fun update(settings: UserSettings): Result<Unit> = runCatchingResult {
        settings.validate()
        settingsFlow.value = settings
    }
}
