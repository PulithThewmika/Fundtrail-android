package com.fundtrail.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.fundtrail.domain.model.LKR
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.FxRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Preferences DataStore backing for [FxRateRepository]: one `fx_rate_<CURRENCY>` key per currency, so a new
 * currency needs no migration and reading one never has to parse the others.
 */
class FxRateRepositoryImpl @Inject constructor(
    private val preferences: DataStore<Preferences>,
) : FxRateRepository {

    override fun observeLastUsedRates(): Flow<Map<String, Long>> = preferences.data.map { prefs ->
        prefs.asMap()
            .mapNotNull { (key, value) ->
                val currency = key.name.removePrefix(KEY_PREFIX).takeIf { it != key.name } ?: return@mapNotNull null
                (value as? Long)?.takeIf { it > 0 }?.let { currency to it }
            }
            .toMap()
    }

    override suspend fun setLastUsedRate(currency: String, rateMicros: Long): Result<Unit> = runCatchingResult {
        require(currency.isNotBlank() && currency != LKR) { "Only a foreign currency has a rate to remember" }
        require(rateMicros > 0) { "FX rate must be positive" }
        preferences.edit { it[longPreferencesKey(KEY_PREFIX + currency)] = rateMicros }
    }

    private companion object {
        const val KEY_PREFIX = "fx_rate_"
    }
}
