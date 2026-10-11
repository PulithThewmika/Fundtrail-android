package com.fundtrail.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SyncStateEntity::class], version = 1, exportSchema = false)
abstract class FundtrailDatabase : RoomDatabase()
