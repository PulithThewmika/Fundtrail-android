package com.fundtrail.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fundtrail.MainActivity
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.repository.ReminderScheduler
import com.fundtrail.domain.model.ReminderMessage
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.ReminderSettingsRepository
import com.fundtrail.domain.repository.TransactionRepository
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.recurring.GetDueRecurringUseCase
import com.fundtrail.domain.usecase.reminder.SelectReminderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * The daily digest worker (#218, #220). Picks ONE message by priority through [SelectReminderUseCase], posts at
 * most one notification on the "Daily reminder" channel, and re-schedules itself for the next day so the time
 * never drifts. If the user is signed out or notifications are off, it quietly does nothing and still reschedules
 * only when the reminder is still enabled.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val auth: AuthRepository,
    private val transactions: TransactionRepository,
    private val getSalaryStatus: GetSalaryStatusUseCase,
    private val getDueRecurring: GetDueRecurringUseCase,
    private val selectReminder: SelectReminderUseCase,
    private val reminderSettings: ReminderSettingsRepository,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = reminderSettings.observe().first()
        // A stale request (toggle turned off, or signed out) posts nothing and stops the chain.
        if (!settings.enabled || auth.currentUserId.first() == null) return Result.success()

        // Each run schedules the next one, so a Room or DataStore read error must not break the chain and leave
        // the reminder silent until the user toggles it off and on. A cancel still has to stop it, so a
        // CancellationException is rethrown rather than swallowed.
        try {
            val today = LocalDate.now(clock.withZone(APP_ZONE))
            val month = YearMonth.from(today)
            val salary = getSalaryStatus(month).first()
            val dueCount = getDueRecurring().first().size
            val loggedToday = transactions.observeMonth(month).first().any {
                it.deletedAt == null && Instant.ofEpochMilli(it.occurredAt).atZone(APP_ZONE).toLocalDate() == today
            }

            val message = selectReminder(salary, dueCount, loggedToday)
            if (message != null && notificationsAllowed()) postNotification(message)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w(TAG, "Skipping today's reminder", error)
        }

        // Re-schedule the next occurrence itself, so the delivery time can't drift (#218).
        scheduler.schedule(settings.minuteOfDay)
        return Result.success()
    }

    private fun notificationsAllowed(): Boolean =
        NotificationManagerCompat.from(applicationContext).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)

    private fun postNotification(message: ReminderMessage) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.notification_channel_daily_reminder),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val (titleRes, textRes, dest) = when (message) {
            ReminderMessage.SALARY_DUE -> Triple(R.string.reminder_salary_title, R.string.reminder_salary_text, "salary")
            ReminderMessage.RECURRING_DUE -> Triple(R.string.reminder_due_title, R.string.reminder_due_text, "due")
            ReminderMessage.NOTHING_LOGGED -> Triple(R.string.reminder_log_title, R.string.reminder_log_text, "log")
        }
        val intent = Intent(
            Intent.ACTION_VIEW,
            "fundtrail://reminder/$dest".toUri(),
            applicationContext,
            MainActivity::class.java,
        )
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            NotificationManagerCompat.from(applicationContext).notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(applicationContext.getString(titleRes))
                    .setContentText(applicationContext.getString(textRes))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build(),
            )
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing more to do.
        }
    }

    companion object {
        const val CHANNEL_ID = "daily-reminder"
        // One id: the digest is at most once a day, so a new one replaces any lingering notification.
        private const val NOTIFICATION_ID = 42
        private const val TAG = "DailyReminderWorker"
    }
}
