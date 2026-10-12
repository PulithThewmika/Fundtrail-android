package com.fundtrail.di

import com.fundtrail.data.repository.AuthRepositoryImpl
import com.fundtrail.data.repository.LocalDataRepositoryImpl
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
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
}
