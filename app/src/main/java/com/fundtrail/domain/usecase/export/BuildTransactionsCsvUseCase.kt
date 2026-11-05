package com.fundtrail.domain.usecase.export

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** FR-116: every record as CSV, one row per transaction, opens in a spreadsheet (A-08). */
class BuildTransactionsCsvUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(): String {
        val txs = transactions.observeAll().first()
        val accountNames = accounts.observeAll().first().associateBy(Account::id) { it.name }
        val categoryNames = categories.observeAll().first().associateBy(Category::id) { it.name }
        return compute(txs, accountNames, categoryNames)
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val HEADER = listOf("Date", "Type", "Amount (LKR)", "Account", "To account", "Category", "Source/Kind", "Note")

        internal fun compute(
            transactions: List<Transaction>,
            accountNames: Map<String, String>,
            categoryNames: Map<String, String>,
        ): String {
            val rows = transactions.map { tx ->
                listOf(
                    DATE_FORMAT.format(Instant.ofEpochMilli(tx.occurredAt).atZone(APP_ZONE)),
                    tx.type.name,
                    BigDecimal.valueOf(tx.amountLkrMinor, 2).toPlainString(),
                    accountNames[tx.accountId] ?: tx.accountId,
                    tx.toAccountId?.let { accountNames[it] ?: it } ?: "",
                    tx.categoryId?.let { categoryNames[it] ?: it } ?: "",
                    when (tx.type) {
                        TransactionType.EXPENSE -> tx.spendKind?.name ?: ""
                        TransactionType.INCOME -> tx.incomeSource?.name ?: ""
                        TransactionType.TRANSFER -> ""
                    },
                    tx.note ?: "",
                )
            }
            return (listOf(HEADER) + rows).joinToString("\r\n") { row -> row.joinToString(",") { escapeCsv(it) } } + "\r\n"
        }

        private fun escapeCsv(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }
    }
}
