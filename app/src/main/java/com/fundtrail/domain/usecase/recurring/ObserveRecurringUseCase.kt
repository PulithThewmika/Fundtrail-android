package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** All recurring templates for the Recurring screen (#203, #204), paused ones included (badged, not hidden). */
class ObserveRecurringUseCase @Inject constructor(private val recurring: RecurringRepository) {
    operator fun invoke(): Flow<List<RecurringTemplate>> = recurring.observeAll()
}
