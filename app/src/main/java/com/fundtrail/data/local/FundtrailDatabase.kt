package com.fundtrail.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fundtrail.data.local.dao.AccountDao
import com.fundtrail.data.local.dao.CategoryDao
import com.fundtrail.data.local.dao.GoalDao
import com.fundtrail.data.local.dao.InvoiceDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.local.dao.TransactionDao
import com.fundtrail.data.local.dao.UserSettingsDao
import com.fundtrail.data.local.entity.AccountEntity
import com.fundtrail.data.local.entity.CategoryEntity
import com.fundtrail.data.local.entity.GoalEntity
import com.fundtrail.data.local.entity.InvoiceEntity
import com.fundtrail.data.local.entity.TransactionEntity
import com.fundtrail.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        SyncStateEntity::class, TransactionEntity::class, AccountEntity::class, CategoryEntity::class,
        GoalEntity::class, UserSettingsEntity::class, InvoiceEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FundtrailDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun goalDao(): GoalDao
    abstract fun userSettingsDao(): UserSettingsDao
}
