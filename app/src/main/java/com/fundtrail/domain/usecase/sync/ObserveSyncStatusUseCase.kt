package com.fundtrail.domain.usecase.sync

import com.fundtrail.domain.repository.SyncStatusRepository
import javax.inject.Inject

class ObserveSyncStatusUseCase @Inject constructor(private val status: SyncStatusRepository) {
    operator fun invoke() = status.observe()
}
