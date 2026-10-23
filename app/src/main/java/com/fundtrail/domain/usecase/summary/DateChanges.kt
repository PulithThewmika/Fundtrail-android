package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.APP_ZONE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/**
 * Emits today (in [APP_ZONE]) now and again at each following midnight, so a screen left open overnight rolls
 * its month window forward. The pattern is lifted from `GetGoalProgressUseCase`; the trend use cases share
 * this one copy (#207).
 */
internal fun Clock.dateChanges(): Flow<LocalDate> = flow {
    val zoned = withZone(APP_ZONE)
    while (true) {
        val today = LocalDate.now(zoned)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant()
        delay(Duration.between(zoned.instant(), nextMidnight).toMillis().coerceAtLeast(1L))
    }
}.distinctUntilChanged()
