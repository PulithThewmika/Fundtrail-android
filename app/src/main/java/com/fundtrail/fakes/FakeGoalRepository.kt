package com.fundtrail.fakes

import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeGoalRepository @Inject constructor() : GoalRepository {

    private val goalFlow = MutableStateFlow<Goal?>(KavinduSampleData.defaultGoal)

    override fun observeActive(): Flow<Goal?> = goalFlow

    override suspend fun save(goal: Goal): Result<Unit> = runCatchingResult {
        goal.validate()
        goalFlow.value = goal
    }

    override suspend fun archive(id: String): Result<Unit> = runCatchingResult {
        val current = goalFlow.value
        require(current != null && current.id == id) { "Goal not found: $id" }
        goalFlow.value = current.copy(isActive = false)
    }
}
