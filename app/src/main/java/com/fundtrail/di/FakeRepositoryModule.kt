package com.fundtrail.di

import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.TransactionRepository
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeTransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO(M2) #88: replace with real implementations
@Module
@InstallIn(SingletonComponent::class)
abstract class FakeRepositoryModule {
    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: FakeTransactionRepository): TransactionRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: FakeAccountRepository): AccountRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: FakeCategoryRepository): CategoryRepository
}
