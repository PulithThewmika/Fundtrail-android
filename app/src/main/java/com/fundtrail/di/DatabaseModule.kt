package com.fundtrail.di

import android.content.Context
import androidx.room.Room
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.local.dao.AccountDao
import com.fundtrail.data.local.dao.CategoryDao
import com.fundtrail.data.local.dao.GoalDao
import com.fundtrail.data.local.dao.InvoiceDao
import com.fundtrail.data.local.dao.RecurringDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.local.dao.TransactionDao
import com.fundtrail.data.local.dao.UserSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FundtrailDatabase =
        Room.databaseBuilder(context, FundtrailDatabase::class.java, "fundtrail.db")
            .fallbackToDestructiveMigration(dropAllTables = true) // Room is a rebuildable cache (§4.4)
            .build()

    @Provides fun provideTransactionDao(db: FundtrailDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideAccountDao(db: FundtrailDatabase): AccountDao = db.accountDao()

    @Provides fun provideCategoryDao(db: FundtrailDatabase): CategoryDao = db.categoryDao()

    @Provides fun provideInvoiceDao(db: FundtrailDatabase): InvoiceDao = db.invoiceDao()

    @Provides fun provideRecurringDao(db: FundtrailDatabase): RecurringDao = db.recurringDao()

    @Provides fun provideSyncStateDao(db: FundtrailDatabase): SyncStateDao = db.syncStateDao()

    @Provides fun provideGoalDao(db: FundtrailDatabase): GoalDao = db.goalDao()

    @Provides fun provideUserSettingsDao(db: FundtrailDatabase): UserSettingsDao = db.userSettingsDao()
}
