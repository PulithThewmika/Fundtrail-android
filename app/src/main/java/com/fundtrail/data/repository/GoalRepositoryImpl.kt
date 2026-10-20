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
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val goalDao: GoalDao,
    private val remoteDataSource: GoalRemoteDataSource,
) : GoalRepository {

    override fun observeActive(): Flow<Goal?> = goalDao.observeActive().map { it?.toDomain() }

    override suspend fun save(goal: Goal): Result<Unit> {
        val validationResult = runCatchingResult { goal.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))

        // Only one active goal per user (schema.md §3.7): archive whatever else is currently active.
        val currentActive = goalDao.getActive()
        val archivedId = currentActive?.id?.takeIf { it != goal.id }

        return when (val res = remoteDataSource.saveGoal(uid, goal, archivedId)) {
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
