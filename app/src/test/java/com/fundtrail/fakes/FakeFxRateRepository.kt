package com.fundtrail.fakes

import com.fundtrail.domain.model.LKR
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.FxRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory stand-in for the Preferences DataStore backing of [FxRateRepository] (#179). */
@Singleton
class FakeFxRateRepository @Inject constructor() : FxRateRepository {
    private val rates = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun observeLastUsedRates(): Flow<Map<String, Long>> = rates

    override suspend fun setLastUsedRate(currency: String, rateMicros: Long): Result<Unit> = runCatchingResult {
        require(currency.isNotBlank() && currency != LKR) { "Only a foreign currency has a rate to remember" }
        require(rateMicros > 0) { "FX rate must be positive" }
        rates.value = rates.value + (currency to rateMicros)
    }
}
