package com.fundtrail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fundtrail.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** [endMillis] is exclusive; build the bounds with `YearMonth.toMillisRange()`. */
    @Query("SELECT * FROM transactions WHERE occurredAt >= :startMillis AND occurredAt < :endMillis ORDER BY occurredAt DESC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<TransactionEntity?>

    /**
     * Category of the newest actual expense on [accountId] whose category is still active, or null.
     * Backs suggestion level 1 (FR-23); expected, income and transfer rows never count.
     */
    @Query(
        """
        SELECT t.categoryId FROM transactions t JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE' AND t.status = 'ACTUAL' AND t.accountId = :accountId AND c.archived = 0
        ORDER BY t.occurredAt DESC, t.id LIMIT 1
        """,
    )
    fun observeMostRecentCategoryId(accountId: String): Flow<String?>

    /** Number of actual expenses per still-active category, across all accounts. Backs suggestion level 2. */
    @Query(
        """
        SELECT t.categoryId AS categoryId, COUNT(*) AS uses FROM transactions t JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE' AND t.status = 'ACTUAL' AND c.archived = 0
        GROUP BY t.categoryId
        """,
    )
    fun observeCategoryUsage(): Flow<List<CategoryUsage>>

    /** Account of the newest actual expense, or null if there is none. Backs the quick-add account default. */
    @Query(
        """
        SELECT accountId FROM transactions
        WHERE type = 'EXPENSE' AND status = 'ACTUAL'
        ORDER BY occurredAt DESC, id LIMIT 1
        """,
    )
    fun observeLastUsedAccountId(): Flow<String?>

    @Upsert
    suspend fun upsertAll(entities: List<TransactionEntity>)

    // SQLite caps bound arguments at 999; one snapshot's removals stay far below that.
    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

/** One row of [TransactionDao.observeCategoryUsage]. */
data class CategoryUsage(val categoryId: String, val uses: Int)
