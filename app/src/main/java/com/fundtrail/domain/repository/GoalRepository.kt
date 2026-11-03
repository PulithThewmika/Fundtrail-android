package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for the user's savings goal.
 * Only one active goal exists at a time.
 */
interface GoalRepository {
    fun observeActive(): Flow<Goal?>
    suspend fun save(goal: Goal): Result<Unit>
    suspend fun archive(id: String): Result<Unit>
}
