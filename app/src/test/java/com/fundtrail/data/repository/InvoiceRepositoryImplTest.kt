package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.InvoiceRemoteDataSource
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class InvoiceRepositoryImplTest {

    private var lastRecordedMarkReceived: Pair<String, Transaction>? = null
    private var lastAddedInvoice: Pair<String, Invoice>? = null

    private val fakeRemoteDataSource = object : InvoiceRemoteDataSource {
        override fun observeInvoices(uid: String): Flow<RemoteSnapshot<Invoice>> = error("unused")
        override fun addInvoice(uid: String, invoice: Invoice): Result<String> {
            lastAddedInvoice = uid to invoice
            return Result.Success(invoice.id)
        }

        override fun updateInvoice(uid: String, invoice: Invoice): Result<Unit> {
            return Result.Success(Unit)
        }

        override fun updateInvoice(uid: String, invoiceId: String, updates: Map<String, Any?>): Result<Unit> {
            return Result.Success(Unit)
        }

        override fun deleteInvoice(uid: String, invoiceId: String): Result<Unit> {
            return Result.Success(Unit)
        }

        override fun markReceived(uid: String, invoiceId: String, transaction: Transaction): Result<Unit> {
            lastRecordedMarkReceived = invoiceId to transaction
            return Result.Success(Unit)
        }
    }

    private val auth = FakeAuthRepository()
    private lateinit var db: FundtrailDatabase
    private lateinit var repo: InvoiceRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java
        ).allowMainThreadQueries().build()

        repo = InvoiceRepositoryImpl(auth, db.invoiceDao(), fakeRemoteDataSource)
        auth.user.value = "u1"
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `add delegates to remote source after validation`() = runBlocking {
        val invoice = Invoice(
            id = "inv_001",
            client = "Acme Corp",
            project = "Website",
            milestone = "Phase 1",
            amountMinor = 100000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 11, 1)
        )

        val result = repo.add(invoice)
        assertTrue(result.isSuccess)
        assertEquals("u1" to invoice, lastAddedInvoice)
    }

    @Test
    fun `add fails when not signed in`() = runBlocking {
        auth.user.value = null
        val invoice = Invoice(
            id = "inv_001",
            client = "Acme Corp",
            project = "Website",
            amountMinor = 100000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 11, 1)
        )

        val result = repo.add(invoice)
        assertTrue(result.isError)
    }

    @Test
    fun `markReceived constructs note with milestone and delegates to remote`() = runBlocking {
        val invoice = Invoice(
            id = "inv_002",
            client = "Globex",
            project = "Mobile App",
            milestone = "Sprint 1",
            amountMinor = 250000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 12, 1)
        )
        db.invoiceDao().upsertAll(listOf(invoice.toEntity()))

        val settlingTx = Transaction(
            id = "tx_settle_01",
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 250000L,
            accountId = "acc_bank",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = 1700000000000L
        )

        val result = repo.markReceived("inv_002", settlingTx)
        assertTrue(result.isSuccess)

        val (invId, tx) = lastRecordedMarkReceived!!
        assertEquals("inv_002", invId)
        assertEquals("Globex - Mobile App - Sprint 1", tx.note)
        assertEquals(IncomeSource.FREELANCE, tx.incomeSource)
        assertEquals("inv_002", tx.invoiceId)
    }

    @Test
    fun `markReceived constructs note without milestone when milestone is null`() = runBlocking {
        val invoice = Invoice(
            id = "inv_003",
            client = "Soylent",
            project = "Branding",
            milestone = null,
            amountMinor = 150000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 12, 1)
        )
        db.invoiceDao().upsertAll(listOf(invoice.toEntity()))

        val settlingTx = Transaction(
            id = "tx_settle_02",
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 150000L,
            accountId = "acc_bank",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = 1700000000000L
        )

        val result = repo.markReceived("inv_003", settlingTx)
        assertTrue(result.isSuccess)

        val (_, tx) = lastRecordedMarkReceived!!
        assertEquals("Soylent - Branding", tx.note)
    }

    @Test
    fun `markReceived fails if invoice is already RECEIVED`() = runBlocking {
        val invoice = Invoice(
            id = "inv_004",
            client = "Umbrella",
            project = "Lab",
            amountMinor = 300000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 12, 1),
            status = InvoiceStatus.RECEIVED,
            receivedTransactionId = "tx_old"
        )
        db.invoiceDao().upsertAll(listOf(invoice.toEntity()))

        val settlingTx = Transaction(
            id = "tx_settle_03",
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 300000L,
            accountId = "acc_bank",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = 1700000000000L
        )

        val result = repo.markReceived("inv_004", settlingTx)
        assertTrue(result.isError)
    }

    @Test
    fun `markReceived fails if settling transaction is not INCOME or ACTUAL`() = runBlocking {
        val invoice = Invoice(
            id = "inv_005",
            client = "Stark",
            project = "Armor",
            amountMinor = 500000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 12, 1)
        )
        db.invoiceDao().upsertAll(listOf(invoice.toEntity()))

        val expenseTx = Transaction(
            id = "tx_wrong",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 500000L,
            accountId = "acc_bank",
            categoryId = "cat_1",
            spendKind = com.fundtrail.domain.model.SpendKind.COMMITTED,
            occurredAt = 1700000000000L
        )

        val result = repo.markReceived("inv_005", expenseTx)
        assertTrue(result.isError)
    }

    @Test
    fun `observeAll returns flow of mapped invoices from Room`() = runBlocking {
        val invoice = Invoice(
            id = "inv_006",
            client = "Wayne",
            project = "Security",
            amountMinor = 800000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 12, 1)
        )
        db.invoiceDao().upsertAll(listOf(invoice.toEntity()))

        val list = repo.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("Wayne", list.first().client)
    }
}
