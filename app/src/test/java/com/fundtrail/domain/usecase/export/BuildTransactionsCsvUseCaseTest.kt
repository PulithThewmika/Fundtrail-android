package com.fundtrail.domain.usecase.export

import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-116. */
class BuildTransactionsCsvUseCaseTest {

    @Test
    fun `one row per transaction, with a header`() {
        val txs = listOf(
            Transaction(
                id = "t1", type = TransactionType.EXPENSE, amountLkrMinor = 150_000L,
                accountId = "acc_main_bank", categoryId = "cat_groceries", spendKind = SpendKind.DISCRETIONARY,
                occurredAt = 1_700_000_000_000L, note = "Milk, eggs",
            ),
        )
        val csv = BuildTransactionsCsvUseCase.compute(
            transactions = txs,
            accountNames = mapOf("acc_main_bank" to "Main Bank"),
            categoryNames = mapOf("cat_groceries" to "Groceries"),
        )
        val lines = csv.trim().split("\r\n")
        assertEquals(2, lines.size) // header + one row
        assertTrue(lines[0].startsWith("Date,Type,Amount"))
        assertTrue(lines[1].contains("EXPENSE"))
        assertTrue(lines[1].contains("1500.00"))
        assertTrue(lines[1].contains("Main Bank"))
        assertTrue(lines[1].contains("Groceries"))
    }

    @Test
    fun `a note with a comma is quoted so it opens correctly in a spreadsheet`() {
        val txs = listOf(
            Transaction(
                id = "t1", type = TransactionType.EXPENSE, amountLkrMinor = 100L,
                accountId = "a", categoryId = "c", spendKind = SpendKind.DISCRETIONARY,
                occurredAt = 1_700_000_000_000L, note = "a, b",
            ),
        )
        val csv = BuildTransactionsCsvUseCase.compute(txs, emptyMap(), emptyMap())
        assertTrue(csv.contains("\"a, b\""))
    }
}
