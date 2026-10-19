package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The quick-add account default comes from Room: the newest actual expense decides, nothing else. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class LastUsedAccountQueryTest {

    private lateinit var db: FundtrailDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private var nextId = 0
    private fun expense(account: String, at: Long, status: TransactionStatus = TransactionStatus.ACTUAL) = Transaction(
        id = "t${nextId++}", type = TransactionType.EXPENSE, status = status, amountLkrMinor = 1_000,
        accountId = account, categoryId = "cat_gym", spendKind = SpendKind.COMMITTED, occurredAt = at,
    )

    private fun save(vararg tx: Transaction) = runBlocking { db.transactionDao().upsertAll(tx.map { it.toEntity() }) }

    private fun lastUsed() = runBlocking { db.transactionDao().observeLastUsedAccountId().first() }

    @Test
    fun noExpensesMeansNoDefault() {
        assertNull(lastUsed())
    }

    @Test
    fun theNewestExpenseByOccurredAtWins() {
        save(expense("acc_cash", at = 200), expense("acc_main_bank", at = 100))

        assertEquals("acc_cash", lastUsed())
    }

    @Test
    fun expectedIncomeAndTransferRowsAreIgnored() {
        save(
            expense("acc_main_bank", at = 100),
            expense("acc_cash", at = 500, status = TransactionStatus.EXPECTED),
            Transaction(
                id = "inc", type = TransactionType.INCOME, amountLkrMinor = 1_000, accountId = "acc_binance",
                incomeSource = IncomeSource.SALARY, occurredAt = 600,
            ),
        )

        assertEquals("acc_main_bank", lastUsed())
    }
}
