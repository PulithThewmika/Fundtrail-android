package com.fundtrail.di

import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.fakes.FakeRecurringRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO(M3): each binding moves to RepositoryModule as its real data layer lands
// (#197 Recurring). Goal and Settings moved there in #161, Invoice in #183.
@Module
@InstallIn(SingletonComponent::class)
abstract class FakeRepositoryModule {
    @Binds @Singleton
    abstract fun bindRecurringRepository(impl: FakeRecurringRepository): RecurringRepository
}
