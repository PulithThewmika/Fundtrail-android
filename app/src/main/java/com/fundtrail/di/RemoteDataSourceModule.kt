package com.fundtrail.di

import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.data.remote.source.AccountRemoteDataSourceImpl
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.data.remote.source.CategoryRemoteDataSourceImpl
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.data.remote.source.TransactionRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindAccountRemoteDataSource(
        impl: AccountRemoteDataSourceImpl
    ): AccountRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCategoryRemoteDataSource(
        impl: CategoryRemoteDataSourceImpl
    ): CategoryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTransactionRemoteDataSource(
        impl: TransactionRemoteDataSourceImpl
    ): TransactionRemoteDataSource
}
