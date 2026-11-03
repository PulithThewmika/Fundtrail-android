package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Domain repository interface for per-user settings on `users/{uid}`. */
interface SettingsRepository {
    fun observe(): Flow<UserSettings>
    suspend fun update(settings: UserSettings): Result<Unit>
}
