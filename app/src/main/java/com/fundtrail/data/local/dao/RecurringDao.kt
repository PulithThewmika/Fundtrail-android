package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    // nextDueDate is stored as ISO-8601 (YYYY-MM-DD), so string order is chronological order.
    @Query("SELECT * FROM recurring ORDER BY nextDueDate ASC, name ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun getById(id: String): RecurringEntity?

    @Upsert
    suspend fun upsertAll(entities: List<RecurringEntity>)

    @Query("DELETE FROM recurring WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM recurring")
    suspend fun deleteAll()
}
