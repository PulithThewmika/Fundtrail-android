package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertFalse(updateMap.containsKey("deletedAt"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TransactionDto toDomain throws IllegalArgumentException for unknown transaction type`() {
        val dto = TransactionDto(
            id = "tx_unknown",
            type = "INVALID_TYPE",
            amountLkrMinor = 1000L,
            accountId = "acc_01"
        )
        dto.toDomain()
    }

    @Test
    fun `TransactionDto toDomain falls back to EXPECTED status for unknown status`() {
        val dto = TransactionDto(
            id = "tx_unkn_stat",
            type = "INCOME",
            status = "UNKNOWN_STATUS",
            amountLkrMinor = 1000L,
            accountId = "acc_01",
            incomeSource = "SALARY"
        )
        val domain = dto.toDomain()
        assertEquals(TransactionStatus.EXPECTED, domain.status)
    }

    @Test
    fun `AccountDto and CategoryDto preserve deletedAt tombstone mapping`() {
        val now = 1700000000000L
        val account = Account(
            id = "acc_del",
            name = "Deleted Acc",
            kind = AccountKind.BANK,
            deletedAt = now
        )
        val accDto = account.toDto()
        assertEquals(now, accDto.deletedAt?.toDate()?.time)
        val accDomain = accDto.toDomain()
        assertEquals(now, accDomain.deletedAt)

        val category = Category(
            id = "cat_del",
            name = "Deleted Cat",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "ic_trash",
            deletedAt = now
        )
        val catDto = category.toDto()
        assertEquals(now, catDto.deletedAt?.toDate()?.time)
        val catDomain = catDto.toDomain()
        assertEquals(now, catDomain.deletedAt)
    }

    @Test
    fun `Invoice toDto and toDomain bidirectional mapping`() {
        val now = 1700000000000L
        val invoice = Invoice(
            id = "inv_01",
            client = "Acme Corp",
            project = "Redesign",
            milestone = "Phase 1",
            amountMinor = 150000L,
            currency = "USD",
            dueDate = LocalDate.of(2026, 11, 15),
            status = InvoiceStatus.RECEIVED,
            receivedTransactionId = "tx_01",
            receivedAt = now,
            lastNotifiedAt = now,
            note = "Invoice for Phase 1",
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            issuedAt = now
        )

        val dto = invoice.toDto()
        assertEquals("inv_01", dto.id)
        assertEquals("Acme Corp", dto.clientName)
        assertEquals("Redesign", dto.projectName)
        assertEquals("Phase 1", dto.milestone)
        assertEquals(150000L, dto.amountMinor)
        assertEquals("USD", dto.currency)
        assertEquals("RECEIVED", dto.status)
        assertEquals("tx_01", dto.receivedTxId)

        assertEquals(invoice, dto.toDomain())
    }

    @Test
    fun `Invoice dto uses the field names in schema section 3_5`() {
        val create = Invoice("i", "C", "P", null, 1L, "LKR", LocalDate.of(2026, 11, 1)).toDto().toMap(isUpdate = false)
        listOf(
            "clientName", "projectName", "dueAt", "issuedAt", "status", "receivedTxId",
            "receivedAt", "lastNotifiedAt", "createdAt", "updatedAt"
        ).forEach { assertTrue("missing $it", create.containsKey(it)) }
        listOf("client", "project", "dueDate", "receivedTransactionId").forEach {
            assertFalse("undocumented field $it", create.containsKey(it))
        }
    }

    @Test
    fun `InvoiceDto create map stamps issuedAt and createdAt and carries the settlement fields`() {
        val dto = InvoiceDto(id = "inv_map_1", clientName = "Globex", projectName = "App Development", amountMinor = 500000L)

        val map = dto.toMap(isUpdate = false)
        assertEquals("Globex", map["clientName"])
        assertEquals("App Development", map["projectName"])
        assertTrue(map.containsKey("createdAt"))
        assertNotNull(map["issuedAt"])
        assertEquals("PENDING", map["status"])
    }

    @Test
    fun `InvoiceDto update map leaves out the settlement fields so a stale edit cannot un-receive an invoice`() {
        val dto = InvoiceDto(
            id = "inv_map_2", clientName = "Globex", projectName = "App Development", amountMinor = 500000L,
            status = "PENDING", receivedTxId = null
        )

        val map = dto.toMap(isUpdate = true)
        listOf("status", "receivedTxId", "receivedAt", "lastNotifiedAt", "createdAt").forEach {
            assertFalse("update must not write $it", map.containsKey(it))
        }
        assertTrue(map.containsKey("updatedAt"))
        assertEquals("Globex", map["clientName"])
        assertFalse("issuedAt is only written when set", map.containsKey("issuedAt"))
        assertEquals(1L, dto.copy(issuedAt = Timestamp(1, 0)).toMap(isUpdate = true).let { (it["issuedAt"] as Timestamp).seconds })
    }

    @Test
    fun `InvoiceDto without a dueAt is rejected rather than given a made-up date`() {
        val dto = InvoiceDto(id = "inv_bad", clientName = "C", projectName = "P", amountMinor = 1L, dueAt = null)
        val error = runCatching { dto.toDomain() }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("inv_bad"))
    }

    @Test
    fun `GoalDto deadline round-trips the same calendar day regardless of device timezone`() {
        val defaultZone = TimeZone.getDefault()
        try {
            val deadline = LocalDate.of(2026, 3, 15)
            val goal = Goal(id = "goal_1", name = "Trip", targetLkrMinor = 1000L, deadline = deadline, baselineSavedMinor = 0L)

            for (zoneId in listOf("UTC", "Pacific/Kiritimati", "Etc/GMT+12")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
                val timestamp = goal.deadlineTimestamp()
                val dto = GoalDto(id = goal.id, name = goal.name, targetLkrMinor = goal.targetLkrMinor, deadline = timestamp)
                assertEquals("zone $zoneId", deadline, dto.toDomain().deadline)
            }
        } finally {
            TimeZone.setDefault(defaultZone)
        }
    }

    @Test
    fun `GoalDto toDomain falls back to startedAt when createdAt hasn't synced back yet`() {
        val startedAt = Timestamp(Date(1700000000000L))
        val dto = GoalDto(id = "goal_2", name = "Fund", targetLkrMinor = 1000L, startedAt = startedAt, createdAt = null)
        assertEquals(startedAt.toDate().time, dto.toDomain().createdAt)
    }

    @Test
    fun `Goal toMap sets startedAt and createdAt only on create`() {
        val goal = Goal(id = "goal_3", name = "Fund", targetLkrMinor = 1000L, deadline = LocalDate.now().plusDays(1), baselineSavedMinor = 0L)
        val now = Instant.ofEpochMilli(1700000000000L)

        val createMap = goal.toMap(isUpdate = false, now = now)
        assertTrue(createMap.containsKey("startedAt"))
        assertTrue(createMap.containsKey("createdAt"))

        val updateMap = goal.toMap(isUpdate = true, now = now)
        assertFalse(updateMap.containsKey("startedAt"))
        assertFalse(updateMap.containsKey("createdAt"))
    }

    @Test
    fun `UserProfileDto toDomain drops displayName and schemaVersion`() {
        val dto = UserProfileDto(displayName = "Kavindu", incomeEstimateMinMinor = 1L, salaryPayday = 10, schemaVersion = 1)
        assertEquals(UserSettings(incomeEstimateMinMinor = 1L, salaryPayday = 10), dto.toDomain())
    }

    @Test
    fun `UserSettings toMap writes displayName only when creating`() {
        val settings = UserSettings(incomeEstimateMinMinor = 1L)

        val createMap = settings.toMap(displayNameOnCreate = "Kavindu")
        assertEquals("Kavindu", createMap["displayName"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("schemaVersion"))

        val updateMap = settings.toMap(displayNameOnCreate = null)
        assertFalse(updateMap.containsKey("displayName"))
        assertFalse(updateMap.containsKey("createdAt"))
        assertFalse(updateMap.containsKey("schemaVersion"))
    }
}
