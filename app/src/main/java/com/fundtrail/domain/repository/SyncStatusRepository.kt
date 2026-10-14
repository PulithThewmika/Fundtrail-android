package com.fundtrail.domain.repository

import com.fundtrail.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncStatusRepository {
    fun observe(): Flow<SyncStatus>
}
