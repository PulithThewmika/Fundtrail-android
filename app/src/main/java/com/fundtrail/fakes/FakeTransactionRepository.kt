package com.fundtrail.fakes

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeTransactionRepository @Inject constructor() : TransactionRepository {

    private val transactionsFlow = MutableStateFlow<List<Transaction>>(KavinduSampleData.sampleTransactions)

    override fun observeMonth(yearMonth: YearMonth): Flow<List<Transaction>> {
        val zoneId = ZoneId.of("Asia/Colombo")
        return transactionsFlow.map { list ->
            list.filter { tx ->
                if (tx.deletedAt != null) return@filter false
                val date = Instant.ofEpochMilli(tx.occurredAt).atZone(zoneId).toLocalDate()
                YearMonth.from(date) == yearMonth
            }
        }
    }

    override fun observeById(id: String): Flow<Transaction?> {
        return transactionsFlow.map { list ->
            list.find { it.id == id && it.deletedAt == null }
        }
    }

    override suspend fun add(transaction: Transaction): Result<Unit> = runCatchingResult {
        transaction.validate()
        require(transactionsFlow.value.none { it.id == transaction.id }) { "Transaction already exists: ${transaction.id}" }
        transactionsFlow.value = transactionsFlow.value + transaction
    }

    override suspend fun update(transaction: Transaction): Result<Unit> = runCatchingResult {
        transaction.validate()
        val currentList = transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id && it.deletedAt == null }
        require(index != -1) { "Transaction not found: ${transaction.id}" }
        currentList[index] = transaction
        transactionsFlow.value = currentList
    }

    override suspend fun delete(id: String): Result<Unit> = runCatchingResult {
        val currentList = transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id && it.deletedAt == null }
        require(index != -1) { "Transaction not found: $id" }
        val existing = currentList[index]
        currentList[index] = existing.copy(deletedAt = System.currentTimeMillis())
        transactionsFlow.value = currentList
    }
}
