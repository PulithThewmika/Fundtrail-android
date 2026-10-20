package com.fundtrail.di

import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.domain.repository.SettingsRepository
import com.fundtrail.fakes.FakeGoalRepository
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO(M3): each binding moves to RepositoryModule as its real data layer lands
// (#161 Goal, #183 Invoice, #197 Recurring). Settings follows whichever lands last.
@Module
@InstallIn(SingletonComponent::class)
abstract class FakeRepositoryModule {
    @Binds @Singleton
    abstract fun bindGoalRepository(impl: FakeGoalRepository): GoalRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: FakeSettingsRepository): SettingsRepository

    @Binds @Singleton
    abstract fun bindRecurringRepository(impl: FakeRecurringRepository): RecurringRepository
}
