package com.fundtrail.fakes

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRecurringRepository @Inject constructor() : RecurringRepository {

    private val templatesFlow = MutableStateFlow<List<RecurringTemplate>>(KavinduSampleData.defaultRecurringTemplates)
    val recordedTransactions = mutableListOf<Transaction>()

    override fun observeAll(): Flow<List<RecurringTemplate>> = templatesFlow

    override suspend fun add(template: RecurringTemplate): Result<Unit> = runCatchingResult {
        template.validate()
        require(templatesFlow.value.none { it.id == template.id }) { "Recurring template already exists: ${template.id}" }
        templatesFlow.value = templatesFlow.value + template
    }

    override suspend fun update(template: RecurringTemplate): Result<Unit> = runCatchingResult {
        template.validate()
        val currentList = templatesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == template.id }
        require(index != -1) { "Recurring template not found: ${template.id}" }
        currentList[index] = template
        templatesFlow.value = currentList
    }

    override suspend fun pause(id: String): Result<Unit> = runCatchingResult {
        val currentList = templatesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        require(index != -1) { "Recurring template not found: $id" }
        currentList[index] = currentList[index].copy(isPaused = true)
        templatesFlow.value = currentList
    }

    override suspend fun confirm(templateId: String, actualAmountMinor: Long, date: Long): Result<Unit> = runCatchingResult {
        val currentList = templatesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == templateId }
        require(index != -1) { "Recurring template not found: $templateId" }
        val template = currentList[index]
        val transaction = Transaction(
            id = "tx_recurring_${templateId}_${date}",
            type = TransactionType.EXPENSE,
            amountLkrMinor = actualAmountMinor,
            accountId = template.accountId,
            categoryId = template.categoryId,
            spendKind = SpendKind.COMMITTED,
            occurredAt = date,
            recurringId = template.id
        )
        transaction.validate()
        // Write the transaction, then advance nextDueDate - one atomic batch.
        recordedTransactions += transaction
        currentList[index] = template.advanced()
        templatesFlow.value = currentList
    }

    override suspend fun skip(templateId: String): Result<Unit> = runCatchingResult {
        val currentList = templatesFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == templateId }
        require(index != -1) { "Recurring template not found: $templateId" }
        currentList[index] = currentList[index].advanced()
        templatesFlow.value = currentList
    }
}
