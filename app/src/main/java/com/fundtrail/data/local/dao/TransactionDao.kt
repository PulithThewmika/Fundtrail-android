package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** [endMillis] is exclusive; build the bounds with `YearMonth.toMillisRange()`. */
    @Query("SELECT * FROM transactions WHERE occurredAt >= :startMillis AND occurredAt < :endMillis ORDER BY occurredAt DESC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<TransactionEntity?>

    @Upsert
    suspend fun upsertAll(entities: List<TransactionEntity>)

    // SQLite caps bound arguments at 999; one snapshot's removals stay far below that.
    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
