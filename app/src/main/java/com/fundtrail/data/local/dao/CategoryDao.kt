package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertAll(entities: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
