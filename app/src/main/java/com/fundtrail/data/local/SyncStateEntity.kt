package com.fundtrail.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One-row table from architecture.md §4.5; wiped on sign-out. Sync work fills it in later. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 0,
    val initialSyncDone: Boolean = false,
)
