package com.fundtrail.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fundtrail.data.local.dao.AccountDao
import com.fundtrail.data.local.dao.CategoryDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.local.dao.TransactionDao
import com.fundtrail.data.local.entity.AccountEntity
import com.fundtrail.data.local.entity.CategoryEntity
import com.fundtrail.data.local.entity.TransactionEntity

@Database(
    entities = [SyncStateEntity::class, TransactionEntity::class, AccountEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FundtrailDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun syncStateDao(): SyncStateDao
}
