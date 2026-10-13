package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class DtoMappingTest {

    @Test
    fun `Account toDto and toDomain bidirectional mapping`() {
        val now = 1700000000000L
        val account = Account(
            id = "acc_01",
            name = "Commercial Bank",
            kind = AccountKind.BANK,
            currency = "LKR",
            openingBalanceLkrMinor = 5000000L,
            openingBalanceAt = now,
            archived = false,
            sortOrder = 1,
            createdAt = now,
            updatedAt = now
        )

        val dto = account.toDto()
        assertEquals("acc_01", dto.id)
        assertEquals("Commercial Bank", dto.name)
        assertEquals("BANK", dto.kind)
        assertEquals("LKR", dto.currency)
        assertEquals(5000000L, dto.openingBalanceLkrMinor)
        assertEquals(now, dto.openingBalanceAt?.toDate()?.time)

        val domain = dto.toDomain()
        assertEquals(account, domain)
    }

    @Test
    fun `AccountDto toDomain handles unknown kind fallback`() {
        val dto = AccountDto(
            id = "acc_02",
            name = "Crypto Wallet",
            kind = "UNKNOWN_FUTURE_KIND",
            currency = "USD"
        )

        val domain = dto.toDomain()
        assertEquals(AccountKind.OTHER, domain.kind)
    }

    @Test
    fun `AccountDto toMap produces correct Firestore map for create and update`() {
        val dto = AccountDto(
            id = "acc_03",
            name = "Savings",
            kind = "SAVINGS",
            openingBalanceAt = Timestamp(Date(1700000000000L))
        )

        val createMap = dto.toMap(isUpdate = false)
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))
        assertEquals("Savings", createMap["name"])
        assertEquals("SAVINGS", createMap["kind"])

        val updateMap = dto.toMap(isUpdate = true)
        assertFalse(updateMap.containsKey("createdAt"))
        assertTrue(updateMap.containsKey("updatedAt"))
    }

    @Test
    fun `Category toDto and toDomain bidirectional mapping`() {
        val now = 1700000000000L
        val category = Category(
            id = "cat_food",
            name = "Food & Dining",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "ic_food",
            archived = false,
            sortOrder = 2,
            createdAt = now,
            updatedAt = now
        )

        val dto = category.toDto()
        assertEquals("cat_food", dto.id)
        assertEquals("Food & Dining", dto.name)
        assertEquals("DISCRETIONARY", dto.defaultSpendKind)

        val domain = dto.toDomain()
        assertEquals(category, domain)
    }

    @Test
    fun `CategoryDto toDomain handles unknown spendKind fallback`() {
        val dto = CategoryDto(
            id = "cat_01",
            name = "Groceries",
            defaultSpendKind = "UNKNOWN_SPEND_KIND",
            icon = "ic_cart"
        )

        val domain = dto.toDomain()
        assertEquals(SpendKind.COMMITTED, domain.defaultSpendKind)
    }

    @Test
    fun `OriginalAmount toDto toDomain and toMap`() {
        val original = OriginalAmount(
            currency = "USD",
            amountMinor = 1000L,
            fxRateMicros = 300000000L
        )

        val dto = original.toDto()
        assertEquals("USD", dto.currency)
        assertEquals(1000L, dto.amountMinor)
        assertEquals(300000000L, dto.fxRateMicros)

        val domain = dto.toDomain()
        assertEquals(original, domain)

        val map = dto.toMap()
        assertEquals("USD", map["currency"])
        assertEquals(1000L, map["amountMinor"])
        assertEquals(300000000L, map["fxRateMicros"])
    }

    @Test
    fun `Transaction EXPENSE toDto and toDomain mapping`() {
        val now = 1700000000000L
        val expense = Transaction(
            id = "tx_exp_1",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 250000L,
            accountId = "acc_01",
            categoryId = "cat_food",
            spendKind = SpendKind.DISCRETIONARY,
            occurredAt = now,
            original = OriginalAmount("USD", 800, 312500000L),
            note = "Dinner at restaurant",
            createdAt = now,
            updatedAt = now
        )

        val dto = expense.toDto()
        assertEquals("EXPENSE", dto.type)
        assertEquals("ACTUAL", dto.status)
        assertEquals("acc_01", dto.accountId)
        assertEquals("cat_food", dto.categoryId)
        assertEquals("DISCRETIONARY", dto.spendKind)
        assertNull(dto.incomeSource)
        assertNull(dto.toAccountId)
        assertNotNull(dto.original)

        val domain = dto.toDomain()
        assertEquals(expense, domain)
    }

    @Test
    fun `Transaction INCOME toDto and toDomain mapping`() {
        val now = 1700000000000L
        val income = Transaction(
            id = "tx_inc_1",
            type = TransactionType.INCOME,
            status = TransactionStatus.EXPECTED,
            amountLkrMinor = 15000000L,
            accountId = "acc_01",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = now,
            invoiceId = "inv_123",
            createdAt = now,
            updatedAt = now
        )

        val dto = income.toDto()
        assertEquals("INCOME", dto.type)
        assertEquals("EXPECTED", dto.status)
        assertEquals("FREELANCE", dto.incomeSource)
        assertEquals("inv_123", dto.invoiceId)
        assertNull(dto.categoryId)

        val domain = dto.toDomain()
        assertEquals(income, domain)
    }

    @Test
    fun `Transaction TRANSFER toDto and toDomain mapping`() {
        val now = 1700000000000L
        val transfer = Transaction(
            id = "tx_trf_1",
            type = TransactionType.TRANSFER,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 1000000L,
            accountId = "acc_01",
            toAccountId = "acc_02",
            occurredAt = now,
            createdAt = now,
            updatedAt = now
        )

        val dto = transfer.toDto()
        assertEquals("TRANSFER", dto.type)
        assertEquals("acc_01", dto.accountId)
        assertEquals("acc_02", dto.toAccountId)

        val domain = dto.toDomain()
        assertEquals(transfer, domain)
    }

    @Test
    fun `TransactionDto toMap produces correct map structure`() {
        val dto = TransactionDto(
            id = "tx_map_1",
            type = "EXPENSE",
            status = "ACTUAL",
            amountLkrMinor = 5000L,
            accountId = "acc_01",
            categoryId = "cat_01",
            spendKind = "COMMITTED",
            original = OriginalAmountDto("USD", 15, 300000000L)
        )

        val createMap = dto.toMap(isUpdate = false)
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))
        assertEquals("EXPENSE", createMap["type"])
        assertNotNull(createMap["original"])

        val updateMap = dto.toMap(isUpdate = true)
        assertFalse(updateMap.containsKey("createdAt"))
        assertTrue(updateMap.containsKey("updatedAt"))
    }
}
