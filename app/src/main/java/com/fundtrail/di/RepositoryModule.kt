package com.fundtrail.di

import com.fundtrail.data.repository.AccountRepositoryImpl
import com.fundtrail.data.repository.AppLockRepositoryImpl
import com.fundtrail.data.repository.AuthRepositoryImpl
import com.fundtrail.data.repository.CategoryRepositoryImpl
import com.fundtrail.data.repository.GoalRepositoryImpl
import com.fundtrail.data.repository.InvoiceRepositoryImpl
import com.fundtrail.data.repository.LocalDataRepositoryImpl
import com.fundtrail.data.repository.SettingsRepositoryImpl
import com.fundtrail.data.repository.SyncStatusRepositoryImpl
import com.fundtrail.data.repository.TransactionRepositoryImpl
import com.fundtrail.data.security.BiometricDeviceSecurity
import com.fundtrail.data.sync.SyncManager
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.AppLockRepository
import com.fundtrail.domain.repository.DeviceSecurityRepository
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.repository.InvoiceRepository
import com.fundtrail.domain.repository.LocalDataRepository
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
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository

    @Binds
    abstract fun bindDeviceSecurityRepository(impl: BiometricDeviceSecurity): DeviceSecurityRepository
}
