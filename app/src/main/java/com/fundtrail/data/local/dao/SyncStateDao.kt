package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.fundtrail.data.local.SyncStateEntity

@Dao
interface SyncStateDao {
    @Upsert
    suspend fun upsert(state: SyncStateEntity)
}
