package com.fundtrail.di

import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.data.remote.source.AccountRemoteDataSourceImpl
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.data.remote.source.CategoryRemoteDataSourceImpl
import com.fundtrail.data.remote.source.GoalRemoteDataSource
import com.fundtrail.data.remote.source.GoalRemoteDataSourceImpl
import com.fundtrail.data.remote.source.InvoiceRemoteDataSource
import com.fundtrail.data.remote.source.InvoiceRemoteDataSourceImpl
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.data.remote.source.TransactionRemoteDataSourceImpl
import com.fundtrail.data.remote.source.UserProfileRemoteDataSource
import com.fundtrail.data.remote.source.UserProfileRemoteDataSourceImpl
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

    @Binds
    @Singleton
    abstract fun bindInvoiceRemoteDataSource(
        impl: InvoiceRemoteDataSourceImpl
    ): InvoiceRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindGoalRemoteDataSource(
        impl: GoalRemoteDataSourceImpl
    ): GoalRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserProfileRemoteDataSource(
        impl: UserProfileRemoteDataSourceImpl
    ): UserProfileRemoteDataSource
}
