package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.GoalDao
import com.fundtrail.data.remote.source.GoalRemoteDataSource
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val goalDao: GoalDao,
    private val remoteDataSource: GoalRemoteDataSource,
) : GoalRepository {

    // Serializes save() so two rapid saves can't both read the same "current active" Room row and
    // archive it twice, each leaving a different goal active (only one signed-in user at a time
    // per process, so one lock for this singleton is enough; there's no cross-process case).
    private val saveLock = Mutex()

    override fun observeActive(): Flow<Goal?> = goalDao.observeActive().map { it?.toDomain() }

    override suspend fun save(goal: Goal): Result<Unit> = saveLock.withLock {
        val validationResult = runCatchingResult { goal.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))

        // The doc may not exist in Room yet for a very recent create whose write hasn't synced
        // back (schema.md §6.4): `isUpdate` is only a best-effort signal from the local mirror.
        val isUpdate = goalDao.get(goal.id) != null

        // Only one active goal per user (schema.md §3.7): archiving the previous one only applies
        // when this save makes a goal active — saving an inactive goal must never touch it.
        val archivedId = goalDao.getActive()?.id?.takeIf { goal.isActive && it != goal.id }

        return when (val res = remoteDataSource.saveGoal(uid, goal, isUpdate, archivedId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun archive(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.archiveGoal(uid, id)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }
}
