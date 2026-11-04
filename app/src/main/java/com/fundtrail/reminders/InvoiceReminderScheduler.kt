package com.fundtrail.reminders

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fundtrail.di.ApplicationScope
import com.fundtrail.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Singleton
class InvoiceReminderScheduler @Inject constructor(
    private val auth: AuthRepository,
    private val workManager: WorkManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            auth.currentUserId.distinctUntilChanged().collectLatest { uid ->
                workManager.cancelUniqueWork(WORK_NAME)
                if (uid != null) {
                    val request = PeriodicWorkRequestBuilder<InvoiceReminderWorker>(1, TimeUnit.DAYS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .build()
                        )
                        .build()
                    workManager.enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request,
                    )
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "invoice-overdue-reminders"
    }
}
