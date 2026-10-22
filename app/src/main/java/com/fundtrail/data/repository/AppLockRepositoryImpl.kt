package com.fundtrail.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Preferences DataStore: one device-only boolean. Not Room (not transactional data, nothing to query) and not
 * Firestore (a lock on one phone must not follow the account to another).
 */
class AppLockRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppLockRepository {

    override val enabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] ?: false }

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> = runCatchingResult {
        dataStore.edit { it[KEY_ENABLED] = enabled }
        Unit
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }
}
