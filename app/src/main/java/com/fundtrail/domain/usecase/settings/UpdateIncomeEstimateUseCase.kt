package com.fundtrail.domain.usecase.settings

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateIncomeEstimateUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /** Reads current settings before writing so unrelated salary fields cannot be overwritten. */
    suspend operator fun invoke(minMinor: Long?, maxMinor: Long?): Result<Unit> {
        val current = settingsRepository.observe().first()
        return settingsRepository.update(
            current.copy(
                incomeEstimateMinMinor = minMinor,
                incomeEstimateMaxMinor = maxMinor,
            ),
        )
    }
}
