package com.fundtrail.data.remote.source

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class RemoteDataSourcesTest {

    private lateinit var mockFirestore: FakeFirestore
    private lateinit var firestoreProvider: Provider<FirebaseFirestore>

    private lateinit var accountRemoteDataSource: AccountRemoteDataSource
    private lateinit var categoryRemoteDataSource: CategoryRemoteDataSource
    private lateinit var transactionRemoteDataSource: TransactionRemoteDataSource

    @Before
    fun setUp() {
        mockFirestore = FakeFirestore()
        firestoreProvider = Provider { mockFirestore.firestore }

        accountRemoteDataSource = AccountRemoteDataSourceImpl(firestoreProvider)
        categoryRemoteDataSource = CategoryRemoteDataSourceImpl(firestoreProvider)
        transactionRemoteDataSource = TransactionRemoteDataSourceImpl(firestoreProvider)
    }

    @Test
    fun `addAccount generates id when blank and returns success immediately`() {
        val account = Account(
            id = "",
            name = "Main Bank",
            kind = AccountKind.BANK
        )

        val result = accountRemoteDataSource.addAccount("user_123", account)
        assertTrue(result.isSuccess)
        val generatedId = (result as Result.Success).data
        assertTrue(generatedId.isNotBlank())
        assertEquals(generatedId, mockFirestore.lastSetDocumentId)
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
    fun `updateAccount performs non-blocking merge write`() {
        val account = Account(
            id = "acc_01",
            name = "Updated Bank Name",
            kind = AccountKind.BANK
        )

        val result = accountRemoteDataSource.updateAccount("user_123", account)
        assertTrue(result.isSuccess)
        assertEquals("acc_01", mockFirestore.lastSetDocumentId)
        assertTrue(mockFirestore.lastSetWasMerge)
    }

    @Test
    fun `deleteAccount performs soft delete write`() {
        val result = accountRemoteDataSource.deleteAccount("user_123", "acc_01")
        assertTrue(result.isSuccess)
        assertEquals("acc_01", mockFirestore.lastSetDocumentId)
        assertTrue(mockFirestore.lastSetWasMerge)
    }

    @Test
    fun `addCategory generates id when blank and returns success`() {
        val category = Category(
            id = "",
            name = "Dining",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "ic_food"
        )

        val result = categoryRemoteDataSource.addCategory("user_123", category)
        assertTrue(result.isSuccess)
        val generatedId = (result as Result.Success).data
        assertTrue(generatedId.isNotBlank())
    }

    @Test
    fun `addTransaction validates transaction and performs non-blocking write`() {
        val transaction = Transaction(
            id = "tx_123",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 50000L,
            accountId = "acc_01",
            categoryId = "cat_food",
            spendKind = SpendKind.DISCRETIONARY,
            occurredAt = System.currentTimeMillis()
        )

        val result = transactionRemoteDataSource.addTransaction("user_123", transaction)
        assertTrue(result.isSuccess)
        assertEquals("tx_123", (result as Result.Success).data)
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
            occurredAt = System.currentTimeMillis()
        )

        val result = transactionRemoteDataSource.addTransaction("user_123", invalidTransaction)
        assertTrue(result.isError)
    }

    @Test
    fun `observeAccounts with blank uid emits empty list`() = runBlocking {
        val flow = accountRemoteDataSource.observeAccounts("")
        val list = flow.first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `observeCategories with blank uid emits empty list`() = runBlocking {
        val flow = categoryRemoteDataSource.observeCategories("")
        val list = flow.first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `observeTransactions with blank uid emits empty list`() = runBlocking {
        val flow = transactionRemoteDataSource.observeTransactions("")
        val list = flow.first()
        assertTrue(list.isEmpty())
    }

    private class FakeFirestore {
        var lastSetDocumentId: String? = null
        var lastSetWasMerge: Boolean = false

        val firestore: FirebaseFirestore = org.mockito.Mockito.mock(FirebaseFirestore::class.java).also { mock ->
            val usersColl = org.mockito.Mockito.mock(CollectionReference::class.java)
            val userDoc = org.mockito.Mockito.mock(DocumentReference::class.java)
            val subColl = org.mockito.Mockito.mock(CollectionReference::class.java)
            val docRef = org.mockito.Mockito.mock(DocumentReference::class.java)
            val dummyTask = org.mockito.Mockito.mock(Task::class.java) as Task<Void>

            org.mockito.Mockito.`when`(mock.collection("users")).thenReturn(usersColl)
            org.mockito.Mockito.`when`(usersColl.document(org.mockito.Mockito.anyString())).thenReturn(userDoc)
            org.mockito.Mockito.`when`(userDoc.collection(org.mockito.Mockito.anyString())).thenReturn(subColl)

            org.mockito.Mockito.`when`(subColl.document()).thenAnswer {
                val autoDoc = org.mockito.Mockito.mock(DocumentReference::class.java)
                val autoId = "generated_auto_id_123"
                org.mockito.Mockito.`when`(autoDoc.id).thenReturn(autoId)
                setupDocRef(autoDoc, autoId, dummyTask)
                autoDoc
            }

            org.mockito.Mockito.`when`(subColl.document(org.mockito.Mockito.anyString())).thenAnswer { invocation ->
                val id = invocation.arguments[0] as String
                val specificDoc = org.mockito.Mockito.mock(DocumentReference::class.java)
                org.mockito.Mockito.`when`(specificDoc.id).thenReturn(id)
                setupDocRef(specificDoc, id, dummyTask)
                specificDoc
            }
        }

        private fun setupDocRef(docRef: DocumentReference, id: String, dummyTask: Task<Void>) {
            org.mockito.Mockito.`when`(docRef.set(org.mockito.Mockito.any())).thenAnswer {
                lastSetDocumentId = id
                lastSetWasMerge = false
                dummyTask
            }
            org.mockito.Mockito.`when`(docRef.set(org.mockito.Mockito.any(), org.mockito.Mockito.any(SetOptions::class.java))).thenAnswer {
                lastSetDocumentId = id
                lastSetWasMerge = true
                dummyTask
            }
        }
    }
}
