package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.FxRateRepository
import javax.inject.Inject

/**
 * Remembers the rate a non-LKR income entry was saved with (#179). Called only after a successful save, so a
 * rate the user typed and abandoned never becomes the prefill.
 */
class SaveLastUsedRateUseCase @Inject constructor(private val rates: FxRateRepository) {
    suspend operator fun invoke(currency: String, rateMicros: Long): Result<Unit> =
        rates.setLastUsedRate(currency, rateMicros)
}
