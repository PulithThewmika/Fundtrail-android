package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    // dueDate is stored as ISO-8601 (YYYY-MM-DD), so string order is chronological order.
    @Query("SELECT * FROM invoices ORDER BY dueDate ASC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): InvoiceEntity?

    @Upsert
    suspend fun upsertAll(entities: List<InvoiceEntity>)

    @Query("DELETE FROM invoices WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM invoices")
    suspend fun deleteAll()
}
