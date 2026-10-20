package com.fundtrail.di

import com.fundtrail.domain.repository.InvoiceRepository
import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.fakes.FakeInvoiceRepository
import com.fundtrail.fakes.FakeRecurringRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO(M3): each binding moves to RepositoryModule as its real data layer lands
// (#183 Invoice, #197 Recurring). Goal and Settings moved there in #161.
@Module
@InstallIn(SingletonComponent::class)
abstract class FakeRepositoryModule {
    @Binds @Singleton
    abstract fun bindInvoiceRepository(impl: FakeInvoiceRepository): InvoiceRepository

    @Binds @Singleton
    abstract fun bindRecurringRepository(impl: FakeRecurringRepository): RecurringRepository
}
