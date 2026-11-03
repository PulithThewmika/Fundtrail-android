package com.fundtrail.data.remote.source

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import javax.inject.Provider

class RemoteDataSourcesTest {

    private lateinit var accountRemoteDataSource: AccountRemoteDataSource
    private lateinit var categoryRemoteDataSource: CategoryRemoteDataSource
    private lateinit var transactionRemoteDataSource: TransactionRemoteDataSource
    private lateinit var invoiceRemoteDataSource: InvoiceRemoteDataSource

    @Before
    fun setUp() {
        val dummyProvider = Provider<FirebaseFirestore> {
            error("Firestore instance should not be invoked in blank uid tests")
        }
        accountRemoteDataSource = AccountRemoteDataSourceImpl(dummyProvider)
        categoryRemoteDataSource = CategoryRemoteDataSourceImpl(dummyProvider)
        transactionRemoteDataSource = TransactionRemoteDataSourceImpl(dummyProvider)
        invoiceRemoteDataSource = InvoiceRemoteDataSourceImpl(dummyProvider)
    }

    @Test
    fun `addAccount with blank uid fails with Error`() {
        val account = Account(
            id = "acc_1",
            name = "Main Bank",
            kind = AccountKind.BANK
        )

        val result = accountRemoteDataSource.addAccount("", account)
        assertTrue(result.isError)
    }

    @Test
    fun `updateAccount with blank uid fails with Error`() {
        val account = Account(
            id = "acc_1",
            name = "Main Bank",
            kind = AccountKind.BANK
        )

        val result = accountRemoteDataSource.updateAccount("", account)
        assertTrue(result.isError)
    }

    @Test
    fun `deleteAccount with blank uid fails with Error`() {
        val result = accountRemoteDataSource.deleteAccount("", "acc_1")
        assertTrue(result.isError)
    }

    @Test
    fun `deleteAccount with blank accountId fails with Error`() {
        val result = accountRemoteDataSource.deleteAccount("user_123", "")
        assertTrue(result.isError)
    }

    @Test
    fun `addCategory with blank uid fails with Error`() {
        val category = Category(
            id = "cat_1",
            name = "Food",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "ic_food"
        )

        val result = categoryRemoteDataSource.addCategory("", category)
        assertTrue(result.isError)
    }

    @Test
    fun `addTransaction invalid model fails with Error`() {
        val invalidTransaction = Transaction(
            id = "tx_invalid",
            type = TransactionType.EXPENSE, // Expense requires categoryId & spendKind
            amountLkrMinor = 50000L,
            accountId = "acc_01",
            categoryId = null,
            spendKind = null,
            occurredAt = 1700000000000L
        )

        val result = transactionRemoteDataSource.addTransaction("user_123", invalidTransaction)
        assertTrue(result.isError)
    }

    @Test
    fun `addTransaction with blank uid fails with Error`() {
        val validTransaction = Transaction(
            id = "tx_valid",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 50000L,
            accountId = "acc_01",
            categoryId = "cat_food",
            spendKind = SpendKind.DISCRETIONARY,
            occurredAt = 1700000000000L
        )

        val result = transactionRemoteDataSource.addTransaction("", validTransaction)
        assertTrue(result.isError)
    }

    @Test
    fun `observeAccounts with blank uid emits empty RemoteSnapshot`() = runBlocking {
        val flow = accountRemoteDataSource.observeAccounts("")
        val snapshot = flow.first()
        assertTrue(snapshot.changes.isEmpty())
        assertTrue(snapshot.isFromCache)
    }

    @Test
    fun `observeCategories with blank uid emits empty RemoteSnapshot`() = runBlocking {
        val flow = categoryRemoteDataSource.observeCategories("")
        val snapshot = flow.first()
        assertTrue(snapshot.changes.isEmpty())
        assertTrue(snapshot.isFromCache)
    }

    @Test
    fun `observeTransactions with blank uid emits empty RemoteSnapshot`() = runBlocking {
        val flow = transactionRemoteDataSource.observeTransactions("")
        val snapshot = flow.first()
        assertTrue(snapshot.changes.isEmpty())
        assertTrue(snapshot.isFromCache)
    }

    @Test
    fun `addInvoice with blank uid fails with Error`() {
        val invoice = Invoice(
            id = "inv_1",
            client = "Client A",
            project = "Project A",
            amountMinor = 100000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 11, 1)
        )
        val result = invoiceRemoteDataSource.addInvoice("", invoice)
        assertTrue(result.isError)
    }

    @Test
    fun `markReceived with blank uid fails with Error`() {
        val settlingTx = Transaction(
            id = "tx_settle",
            type = TransactionType.INCOME,
            amountLkrMinor = 100000L,
            accountId = "acc_01",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = 1700000000000L
        )
        val result = invoiceRemoteDataSource.markReceived("", "inv_1", settlingTx)
        assertTrue(result.isError)
    }

    @Test
    fun `observeInvoices with blank uid emits empty RemoteSnapshot`() = runBlocking {
        val flow = invoiceRemoteDataSource.observeInvoices("")
        val snapshot = flow.first()
        assertTrue(snapshot.changes.isEmpty())
        assertTrue(snapshot.isFromCache)
    }
}
