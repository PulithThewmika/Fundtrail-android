package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.repository.FxRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The rate to prefill into Rate mode for each currency, labelled "last used" and always editable (#179). */
class ObserveLastUsedRatesUseCase @Inject constructor(private val rates: FxRateRepository) {
    operator fun invoke(): Flow<Map<String, Long>> = rates.observeLastUsedRates()
}
