package com.fundtrail.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fundtrail.data.repository.toDomain
import com.fundtrail.data.repository.toEntity
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.fakes.KavinduSampleData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: FundtrailDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FundtrailDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observeBetween_returnsMonthNewestFirst_andRoundTripsOriginal() = runBlocking {
        val dao = db.transactionDao()
        val sample = KavinduSampleData.sampleTransactions
        val lastMinute = LocalDateTime.of(2026, 10, 31, 23, 59).atZone(APP_ZONE).toInstant().toEpochMilli()
        val nextMonth = LocalDateTime.of(2026, 11, 1, 0, 0).atZone(APP_ZONE).toInstant().toEpochMilli()
        val edge = sample.first().copy(id = "tx_edge", occurredAt = lastMinute)
        val outside = sample.first().copy(id = "tx_nov", occurredAt = nextMonth)
        dao.upsertAll((sample + edge + outside).map { it.toEntity() })

        val (start, end) = YearMonth.of(2026, 10).toMillisRange()
        val oct = dao.observeBetween(start, end).first().map { it.toDomain() }

        assertEquals(sample.size + 1, oct.size)
        assertEquals("tx_edge", oct.first().id)
        assertEquals(oct.sortedByDescending { it.occurredAt }, oct)
        assertEquals(sample.toSet(), (oct - oct.first()).toSet())
    }

    @Test
    fun upsertReplaces_deleteByIds_deleteAll() = runBlocking {
        val dao = db.transactionDao()
        val tx = KavinduSampleData.sampleTransactions.first()
        dao.upsertAll(listOf(tx.toEntity()))
        dao.upsertAll(listOf(tx.copy(note = "edited").toEntity()))
        assertEquals("edited", dao.observeById(tx.id).first()?.note)

        dao.deleteByIds(listOf(tx.id))
        assertNull(dao.observeById(tx.id).first())

        db.accountDao().upsertAll(KavinduSampleData.defaultAccounts.map { it.toEntity() })
        db.categoryDao().upsertAll(KavinduSampleData.defaultCategories.map { it.toEntity() })
        assertEquals(KavinduSampleData.defaultAccounts, db.accountDao().observeAll().first().map { it.toDomain() })
        db.accountDao().deleteAll()
        db.categoryDao().deleteAll()
        assertTrue(db.accountDao().observeAll().first().isEmpty())
        assertTrue(db.categoryDao().observeAll().first().isEmpty())
    }
}
