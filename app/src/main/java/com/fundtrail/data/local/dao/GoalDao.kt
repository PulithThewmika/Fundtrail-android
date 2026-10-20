package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    /** Most recently created active goal. More than one active row can arrive from two devices; the newest wins. */
    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActive(): GoalEntity?

    @Upsert
    suspend fun upsertAll(entities: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
