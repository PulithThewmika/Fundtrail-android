package com.fundtrail

import android.app.Application
import com.fundtrail.data.sync.DefaultDataSeeder
import com.fundtrail.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FundtrailApplication : Application() {
    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var defaultDataSeeder: DefaultDataSeeder

    override fun onCreate() {
        super.onCreate()
        syncManager.start()
        defaultDataSeeder.start()
    }
}
