package com.fundtrail.domain.repository

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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
     *
     * [expectedDueDate] is the occurrence the caller is acting on, as shown on screen. The write is refused when
     * the template has moved on since, so a second tap on a stale list cannot book another month (#200).
     */
    suspend fun confirm(templateId: String, actualAmountMinor: Long, date: Long, expectedDueDate: LocalDate): Result<Unit>

    /**
     * Skips the due item for [templateId]: advance `nextDueDate` by one month, no transaction.
     * [expectedDueDate] guards a stale caller the same way [confirm] does.
     */
    suspend fun skip(templateId: String, expectedDueDate: LocalDate): Result<Unit>
}
