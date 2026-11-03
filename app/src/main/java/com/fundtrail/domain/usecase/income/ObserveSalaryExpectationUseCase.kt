package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.SalaryExpectation
import com.fundtrail.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The expected salary and payday from settings (A-12). The month's status is [GetSalaryStatusUseCase]. */
class ObserveSalaryExpectationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<SalaryExpectation> =
        settingsRepository.observe().map { SalaryExpectation(it.expectedSalaryMinor, it.salaryPayday) }
}
