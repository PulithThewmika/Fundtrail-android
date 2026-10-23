package com.fundtrail.di

import com.fundtrail.data.repository.AccountRepositoryImpl
import com.fundtrail.data.repository.AuthRepositoryImpl
import com.fundtrail.data.repository.CategoryRepositoryImpl
import com.fundtrail.data.repository.GoalRepositoryImpl
import com.fundtrail.data.repository.InvoiceRepositoryImpl
import com.fundtrail.data.repository.LocalDataRepositoryImpl
import com.fundtrail.data.repository.FxRateRepositoryImpl
import com.fundtrail.data.repository.RecurringRepositoryImpl
import com.fundtrail.data.repository.ReminderSettingsRepositoryImpl
import com.fundtrail.data.repository.SettingsRepositoryImpl
import com.fundtrail.data.repository.SyncStatusRepositoryImpl
import com.fundtrail.data.repository.TransactionRepositoryImpl
import com.fundtrail.data.sync.SyncManager
import com.fundtrail.domain.repository.ReminderScheduler
import com.fundtrail.reminders.WorkManagerReminderScheduler
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.repository.InvoiceRepository
import com.fundtrail.domain.repository.LocalDataRepository
import com.fundtrail.domain.repository.FxRateRepository
import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.domain.repository.ReminderSettingsRepository
import com.fundtrail.domain.repository.SettingsRepository
import com.fundtrail.domain.repository.SyncRepository
import com.fundtrail.domain.repository.SyncStatusRepository
import com.fundtrail.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindLocalDataRepository(impl: LocalDataRepositoryImpl): LocalDataRepository

    @Binds @Singleton
    abstract fun bindSyncRepository(impl: SyncManager): SyncRepository

    @Binds @Singleton
    abstract fun bindSyncStatusRepository(impl: SyncStatusRepositoryImpl): SyncStatusRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds @Singleton
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindFxRateRepository(impl: FxRateRepositoryImpl): FxRateRepository

    @Binds @Singleton
    abstract fun bindReminderSettingsRepository(impl: ReminderSettingsRepositoryImpl): ReminderSettingsRepository

    @Binds @Singleton
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
