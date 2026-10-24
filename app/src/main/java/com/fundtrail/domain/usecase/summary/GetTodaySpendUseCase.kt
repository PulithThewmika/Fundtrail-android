package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * What was spent today, in LKR minor units (#226): actual expenses dated today in [APP_ZONE]. Transfers and
 * expected entries never count, the same rule as the monthly summary. Re-emits when an entry changes and
 * when the day rolls over at midnight, so the number never goes stale.
 */
class GetTodaySpendUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Long> =
        dateChanges().flatMapLatest { today ->
            transactions.observeMonth(YearMonth.from(today)).map { total(it, today) }
        }.distinctUntilChanged()

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

    companion object {
        internal fun total(all: List<Transaction>, today: LocalDate): Long =
            all.filter {
                it.deletedAt == null &&
                    it.status == TransactionStatus.ACTUAL &&
                    it.type == TransactionType.EXPENSE &&
                    Instant.ofEpochMilli(it.occurredAt).atZone(APP_ZONE).toLocalDate() == today
            }.sumOf { it.amountLkrMinor }
    }
}
