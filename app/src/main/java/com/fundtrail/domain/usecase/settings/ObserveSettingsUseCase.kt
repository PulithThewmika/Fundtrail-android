package com.fundtrail.domain.usecase.settings

import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<UserSettings> = settingsRepository.observe()
}
