package com.fundtrail.domain.repository

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/** Domain repository interface for recurring expense/income templates. */
interface RecurringRepository {
    fun observeAll(): Flow<List<RecurringTemplate>>
    suspend fun add(template: RecurringTemplate): Result<Unit>
    suspend fun update(template: RecurringTemplate): Result<Unit>
    suspend fun pause(id: String): Result<Unit>

    /** Soft delete (sets `deletedAt`); the sync mirror then removes the template from Room. */
    suspend fun delete(id: String): Result<Unit>

    /**
     * Confirms the due item for [templateId] with [actualAmountMinor] on [date].
     * One atomic batch: write the transaction, then advance `nextDueDate` by one month.
     */
    suspend fun confirm(templateId: String, actualAmountMinor: Long, date: Long): Result<Unit>

    /** Skips the due item for [templateId]: advance `nextDueDate` by one month, no transaction. */
    suspend fun skip(templateId: String): Result<Unit>
}
