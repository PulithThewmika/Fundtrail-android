package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.repository.RecurringRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

/**
 * The recurring items due right now (#200, #201): not paused and `nextDueDate <= today` in [APP_ZONE]. Nothing
 * is generated or stored, so nothing can be duplicated - a due item is derived every time from the template.
 *
 * Only the earliest unpaid occurrence of each template shows, because `nextDueDate` is a single cursor; after a
 * confirm or skip advances it, the flow re-emits with the next missed month, so a gap is caught up one at a time
 * (A-03). The window re-checks at midnight so an item appears the day it falls due without reopening the screen.
 */
class GetDueRecurringUseCase @Inject constructor(
    private val recurring: RecurringRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<RecurringTemplate>> =
        combine(recurring.observeAll(), dateChanges()) { templates, today ->
            templates.filter { it.isDue(today) }.sortedWith(compareBy({ it.nextDueDate }, { it.name }))
        }

    /** Emits today (in [APP_ZONE]) now and again at each following midnight. Copied from `GetGoalProgressUseCase`. */
    private fun dateChanges(): Flow<LocalDate> = flow {
        val zoned = clock.withZone(APP_ZONE)
        while (true) {
            val today = LocalDate.now(zoned)
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant()
            delay(Duration.between(zoned.instant(), nextMidnight).toMillis().coerceAtLeast(1L))
        }
    }.distinctUntilChanged()
}
