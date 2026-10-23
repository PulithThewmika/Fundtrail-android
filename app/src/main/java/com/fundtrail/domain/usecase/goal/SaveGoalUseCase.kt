package com.fundtrail.domain.usecase.goal

import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.GoalRepository
import javax.inject.Inject

class SaveGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(goal: Goal): Result<Unit> = repository.save(goal)
}
