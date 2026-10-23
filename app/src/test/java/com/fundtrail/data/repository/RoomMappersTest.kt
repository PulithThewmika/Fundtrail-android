package com.fundtrail.data.repository

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.KavinduSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RoomMappersTest {

    private val fullTransaction = Transaction(
        id = "tx_full",
        type = TransactionType.INCOME,
        status = TransactionStatus.EXPECTED,
        amountLkrMinor = 1_492_250L,
        accountId = "acc_main_bank",
        incomeSource = IncomeSource.ADSENSE,
        occurredAt = 1_790_000_000_000L,
        original = OriginalAmount(currency = "USD", amountMinor = 5_000L, fxRateMicros = 298_450_000L),
        note = "AdSense estimate",
        recurringId = "rec_adsense",
        expectedAmountLkrMinor = 1_500_000L,
        invoiceId = "inv_1",
        createdAt = 1L,
        updatedAt = 2L,
    )

    @Test
    fun `transaction with every field round-trips`() {
        assertEquals(fullTransaction, fullTransaction.toEntity().toDomain())
    }

    @Test
    fun `transaction without original or optionals round-trips`() {
        val tx = Transaction(
            id = "tx_min",
            type = TransactionType.TRANSFER,
            amountLkrMinor = 1_000_000L,
            accountId = "acc_main_bank",
            toAccountId = "acc_secondary_bank",
            occurredAt = 1L,
        )
        assertEquals(tx, tx.toEntity().toDomain())
    }

    @Test
    fun `all sample transactions round-trip`() {
        KavinduSampleData.sampleTransactions.forEach { assertEquals(it, it.toEntity().toDomain()) }
    }

    @Test
    fun `deletedAt is not stored`() {
        assertNull(fullTransaction.copy(deletedAt = 99L).toEntity().toDomain().deletedAt)
    }

    @Test
    fun `account round-trips`() {
        val account = Account(
            id = "acc_savings",
            name = "MacBook fund",
            kind = AccountKind.SAVINGS,
            currency = "LKR",
            openingBalanceLkrMinor = 1_120_000L,
            openingBalanceAt = 5L,
            archived = true,
            sortOrder = 4,
            createdAt = 1L,
            updatedAt = 2L,
        )
        assertEquals(account, account.toEntity().toDomain())
        KavinduSampleData.defaultAccounts.forEach { assertEquals(it, it.toEntity().toDomain()) }
    }

    @Test
    fun `category round-trips`() {
        val category = Category(
            id = "cat_rent",
            name = "Rent",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "home",
            archived = true,
            sortOrder = 1,
            createdAt = 1L,
            updatedAt = 2L,
        )
        assertEquals(category, category.toEntity().toDomain())
        KavinduSampleData.defaultCategories.forEach { assertEquals(it, it.toEntity().toDomain()) }
    }

    @Test
    fun `invoice round-trips`() {
        val invoice = Invoice(
            id = "inv_01",
            client = "Initech",
            project = "TPS Reports",
            milestone = "Milestone 1",
            amountMinor = 250000L,
            currency = "USD",
            dueDate = LocalDate.of(2026, 12, 1),
            status = InvoiceStatus.PENDING,
            createdAt = 1L,
            updatedAt = 2L,
            issuedAt = 3L
        )
        assertEquals(invoice, invoice.toEntity().toDomain())
        KavinduSampleData.defaultInvoices.forEach { assertEquals(it, it.toEntity().toDomain()) }
    }

    @Test
    fun `goal round-trips, including the deadline and server timestamps`() {
        val goal = KavinduSampleData.defaultGoal.copy(createdAt = 1L, updatedAt = 2L, isActive = false)
        assertEquals(goal, goal.toEntity().toDomain())
    }

    @Test
    fun `user settings entity round-trips into the domain model`() {
        val entity = com.fundtrail.data.local.entity.UserSettingsEntity(
            displayName = "Kavindu",
            incomeEstimateMinMinor = KavinduSampleData.defaultSettings.incomeEstimateMinMinor,
            incomeEstimateMaxMinor = KavinduSampleData.defaultSettings.incomeEstimateMaxMinor,
            expectedSalaryMinor = KavinduSampleData.defaultSettings.expectedSalaryMinor,
            salaryPayday = KavinduSampleData.defaultSettings.salaryPayday,
            schemaVersion = 1,
            createdAt = 1L,
            updatedAt = 2L,
        )
        assertEquals(KavinduSampleData.defaultSettings, entity.toDomain())
    }

    @Test
    fun `recurring template round-trips, without the deletedAt tombstone`() {
        val template = RecurringTemplate(
            id = "rec_rent", name = "Rent", categoryId = "cat_rent", accountId = "acc_bank",
            expectedAmountMinor = 3_400_000L, isVariableAmount = true, dayOfMonth = 1,
            nextDueDate = LocalDate.of(2026, 11, 1), isPaused = true, createdAt = 1L, updatedAt = 2L,
        )
        assertEquals(template, template.toEntity().toDomain())
        assertNull(template.copy(deletedAt = 3L).toEntity().toDomain().deletedAt)
        KavinduSampleData.defaultRecurringTemplates.forEach { assertEquals(it, it.toEntity().toDomain()) }
    }
}
