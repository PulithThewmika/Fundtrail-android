package com.fundtrail.domain.usecase.settings

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Saves the whole Income settings form in one write, so the salary and estimate halves can't overwrite each other. */
class UpdateIncomeSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /** @param salaryPayday day of month, 1-28. Amounts are LKR minor units; null clears the value. */
    suspend operator fun invoke(minMinor: Long?, maxMinor: Long?, expectedSalaryMinor: Long?, salaryPayday: Int): Result<Unit> {
        val current = settingsRepository.observe().first()
        return settingsRepository.update(
            current.copy(
                incomeEstimateMinMinor = minMinor,
                incomeEstimateMaxMinor = maxMinor,
                expectedSalaryMinor = expectedSalaryMinor,
                salaryPayday = salaryPayday,
            ),
        )
    }
}
