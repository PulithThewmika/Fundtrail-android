package com.fundtrail.domain.usecase.goal

import com.fundtrail.domain.repository.GoalRepository
import javax.inject.Inject

/** The user's one active goal, or null when none is set (EPIC-012/T4). */
class ObserveActiveGoalUseCase @Inject constructor(private val goals: GoalRepository) {
    operator fun invoke() = goals.observeActive()
}
