package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.RecurringDao
import com.fundtrail.data.remote.source.RecurringRemoteDataSource
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recurring templates (schema.md §3.6). Reads come from the Room mirror; writes go to Firestore and come back
 * through the sync listener (ADR-0004).
 *
 * `confirm`/`skip` only act on a template that is due, and never act twice on the same due date. Room alone
 * can't guarantee the second part: the advanced `nextDueDate` reaches Room only when the listener fires, so a
 * quick second tap would still read the old cursor. [advancedCursors] remembers each cursor this process has
 * already moved, and the confirmed transaction's id is derived from the month it settles (schema.md §5), so even a write
 * that slips through lands on the same document instead of creating a second expense.
 */
@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val recurringDao: RecurringDao,
    private val remoteDataSource: RecurringRemoteDataSource,
    private val clock: Clock,
) : RecurringRepository {

    private val cursorLock = Mutex()

    /** Last cursor this process moved each template to, keyed by "uid/templateId". */
    private val advancedCursors = mutableMapOf<String, LocalDate>()

    override fun observeAll(): Flow<List<RecurringTemplate>> =
        recurringDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun add(template: RecurringTemplate): Result<Unit> {
        val validation = runCatchingResult { template.validate() }
        if (validation is Result.Error) return validation
        val uid = auth.currentUserId.first() ?: return notSignedIn()
        return when (val res = remoteDataSource.addRecurring(uid, template)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun update(template: RecurringTemplate): Result<Unit> {
        val validation = runCatchingResult { template.validate() }
        if (validation is Result.Error) return validation
        val uid = auth.currentUserId.first() ?: return notSignedIn()
        return remoteDataSource.updateRecurring(uid, template)
    }

    override suspend fun pause(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return notSignedIn()
        return remoteDataSource.pauseRecurring(uid, id)
    }

    override suspend fun delete(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return notSignedIn()
        return remoteDataSource.deleteRecurring(uid, id)
    }

    override suspend fun confirm(templateId: String, actualAmountMinor: Long, date: Long): Result<Unit> =
        advanceDue(templateId) { uid, template, dueDate ->
            if (!template.isVariableAmount && actualAmountMinor != template.expectedAmountMinor) {
                return@advanceDue Result.Error(
                    IllegalArgumentException("${template.name} has a fixed amount; it can't be confirmed with a different one")
                )
            }
            val transaction = Transaction(
                id = confirmedTransactionId(templateId, dueDate),
                type = TransactionType.EXPENSE,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = actualAmountMinor,
                accountId = template.accountId,
                categoryId = template.categoryId,
                // A recurring cost is a commitment by definition, whatever the category's default type is.
                spendKind = SpendKind.COMMITTED,
                occurredAt = date,
                recurringId = templateId,
                expectedAmountLkrMinor = template.expectedAmountMinor,
            )
            remoteDataSource.confirm(uid, templateId, transaction, template.advanced().nextDueDate)
        }

    override suspend fun skip(templateId: String): Result<Unit> =
        advanceDue(templateId) { uid, template, _ ->
            remoteDataSource.skip(uid, templateId, template.advanced().nextDueDate)
        }

    /**
     * Runs [write] for the due item of [templateId], with [RecurringTemplate.nextDueDate] already taking any cursor
     * this process advanced but Room hasn't caught up with. Serialised, so two quick taps can't both pass the check.
     */
    private suspend fun advanceDue(
        templateId: String,
        write: suspend (uid: String, template: RecurringTemplate, dueDate: LocalDate) -> Result<Unit>,
    ): Result<Unit> = cursorLock.withLock {
        val uid = auth.currentUserId.first() ?: return@withLock notSignedIn()
        val entity = recurringDao.getById(templateId)
            ?: return@withLock Result.Error(IllegalArgumentException("Recurring template not found locally (not synced yet?): $templateId"))

        val key = "$uid/$templateId"
        val stored = entity.toDomain()
        val cursor = advancedCursors[key]?.takeIf { it.isAfter(stored.nextDueDate) } ?: stored.nextDueDate
        val template = stored.copy(nextDueDate = cursor)

        val today = LocalDate.now(clock.withZone(APP_ZONE))
        if (template.isPaused) {
            return@withLock Result.Error(IllegalStateException("${template.name} is paused"))
        }
        if (!template.isDue(today)) {
            return@withLock Result.Error(IllegalStateException("${template.name} is not due until $cursor"))
        }

        val result = write(uid, template, cursor)
        if (result is Result.Success) advancedCursors[key] = template.advanced().nextDueDate
        result
    }

    private fun notSignedIn(): Result.Error = Result.Error(IllegalStateException("Not signed in"))

    private companion object {
        private val DUE_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")

        /** `{recurringId}_{yyyyMM}` of the due month (schema.md §5): a repeated confirm writes the same document. */
        fun confirmedTransactionId(templateId: String, dueDate: LocalDate) = "${templateId}_${dueDate.format(DUE_MONTH)}"
    }
}
