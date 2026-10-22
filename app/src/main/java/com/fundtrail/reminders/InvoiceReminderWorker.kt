package com.fundtrail.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fundtrail.MainActivity
import com.fundtrail.R
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.InvoiceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class InvoiceReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val auth: AuthRepository,
    private val invoices: InvoiceRepository,
    private val clock: Clock,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (auth.currentUserId.first() == null || !notificationsAllowed()) return Result.success()

        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        val now = clock.millis()
        val overdueInvoices = invoices.observeAll().first()
            .filter { it.isOverdue(today) && shouldNotify(it, now) }
        for (invoice in overdueInvoices) {
                if (!postNotification(invoice, today)) continue
                when (val update = invoices.updateLastNotifiedAt(invoice.id, now)) {
                    is com.fundtrail.domain.model.Result.Error -> return Result.retry()
                    is com.fundtrail.domain.model.Result.Success -> Unit
                }
        }
        return Result.success()
    }

    private fun shouldNotify(invoice: Invoice, now: Long): Boolean =
        invoice.lastNotifiedAt == null || now - invoice.lastNotifiedAt >= THREE_DAYS_MILLIS

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun postNotification(invoice: Invoice, today: LocalDate): Boolean {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.notification_channel_invoices),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }
        val overdueDays = ChronoUnit.DAYS.between(invoice.dueDate, today)
        val intent = Intent(
            Intent.ACTION_VIEW,
            "fundtrail://invoice/${invoice.id}".toUri(),
            applicationContext,
            MainActivity::class.java,
        )
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            invoice.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return try {
            NotificationManagerCompat.from(applicationContext).notify(
                invoice.id.hashCode(),
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(applicationContext.getString(R.string.notification_invoice_title))
                    .setContentText(
                        applicationContext.getString(
                            R.string.notification_invoice_overdue,
                            invoice.client,
                            overdueDays,
                        )
                    )
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build(),
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "invoices"
        private const val THREE_DAYS_MILLIS = 3 * 24 * 60 * 60 * 1000L
    }
}
