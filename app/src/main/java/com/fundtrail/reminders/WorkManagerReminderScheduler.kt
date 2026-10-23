package com.fundtrail.reminders

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.repository.ReminderScheduler
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-backed [ReminderScheduler] (#219). A single unique `daily-reminder` [androidx.work.OneTimeWorkRequest]
 * with [ExistingWorkPolicy.REPLACE] and an initial delay to the next occurrence of the chosen time; the worker
 * re-schedules the following day, so the delivery time cannot drift the way a periodic request's can.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock,
) : ReminderScheduler {

    override fun schedule(minuteOfDay: Int) {
        val delay = delayToNext(minuteOfDay)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf(KEY_MINUTE to minuteOfDay))
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun delayToNext(minuteOfDay: Int): Duration {
        val zoned = clock.withZone(APP_ZONE)
        val now = LocalDate.now(zoned).atTime(LocalTime.now(zoned))
        val target = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        var next = now.toLocalDate().atTime(target)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now.atZone(APP_ZONE), next.atZone(APP_ZONE))
    }

    companion object {
        const val WORK_NAME = "daily-reminder"
        const val KEY_MINUTE = "minute_of_day"

        /**
         * The delay from [nowMinuteOfDay] to the next [targetMinuteOfDay], in minutes (0..1439). Pulled out as a
         * pure function so the "changing the time reschedules without drift" rule is unit-testable (#218 AC).
         */
        fun minutesUntilNext(nowMinuteOfDay: Int, targetMinuteOfDay: Int): Int {
            val diff = targetMinuteOfDay - nowMinuteOfDay
            return if (diff > 0) diff else diff + 24 * 60
        }
    }
}
