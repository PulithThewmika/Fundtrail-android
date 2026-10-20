package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 0")
    suspend fun get(): UserSettingsEntity?

    @Upsert
    suspend fun upsert(entity: UserSettingsEntity)

    @Query("DELETE FROM user_settings")
    suspend fun deleteAll()
}
