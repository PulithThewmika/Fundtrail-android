package com.fundtrail.di

import android.content.Context
import androidx.room.Room
import com.fundtrail.data.local.FundtrailDatabase
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
}
