package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * The rate last used for each non-LKR currency, so a repeat entry does not have to be retyped (#179).
 *
 * A convenience, not a record: the figure a transaction keeps is its own locked `fxRateMicros`, and nothing
 * here is ever read back into a stored amount. It is small, per-device settings data, so it lives in
 * Preferences DataStore rather than Room or Firestore (Docs/design/multi-currency.md).
 */
interface FxRateRepository {
    /** Currency code to rate in micros; a currency with no entry yet is absent from the map. */
    fun observeLastUsedRates(): Flow<Map<String, Long>>

    suspend fun setLastUsedRate(currency: String, rateMicros: Long): Result<Unit>
}
