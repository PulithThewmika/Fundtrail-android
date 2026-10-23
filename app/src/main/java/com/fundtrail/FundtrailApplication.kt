package com.fundtrail

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fundtrail.data.sync.DefaultDataSeeder
import com.fundtrail.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FundtrailApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var defaultDataSeeder: DefaultDataSeeder
    @Inject lateinit var invoiceReminderScheduler: com.fundtrail.reminders.InvoiceReminderScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncManager.start()
        defaultDataSeeder.start()
        invoiceReminderScheduler.start()
    }
}
