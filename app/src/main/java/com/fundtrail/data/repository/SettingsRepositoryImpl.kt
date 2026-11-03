package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.local.dao.UserSettingsDao
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.data.remote.source.UserProfileRemoteDataSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val settingsDao: UserSettingsDao,
    private val syncStateDao: SyncStateDao,
    private val remoteDataSource: UserProfileRemoteDataSource,
) : SettingsRepository {

    override fun observe(): Flow<UserSettings> = settingsDao.observe().map { it?.toDomain() ?: UserSettings() }

    override suspend fun update(settings: UserSettings): Result<Unit> {
        val validationResult = runCatchingResult { settings.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))

        val displayNameOnCreate = if (settingsDao.get() == null) {
            // The profile doc might exist remotely but not have synced down yet; writing without
            // `createdAt` then would silently fail the rules' create check, so wait it out.
            val synced = syncStateDao.observe().first()?.initialSyncDone == true
            if (!synced) {
                return Result.Error(IllegalStateException("Still syncing settings; try again shortly"))
            }
            auth.currentUserDisplayName ?: "FundTrail user"
        } else {
            null
        }

        return when (val res = remoteDataSource.writeProfile(uid, settings.toMap(displayNameOnCreate))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }
}
