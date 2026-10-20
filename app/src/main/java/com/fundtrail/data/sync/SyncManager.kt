package com.fundtrail.data.sync

import android.util.Log
import androidx.room.withTransaction
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.local.SyncStateEntity
import com.fundtrail.data.remote.model.ChangeType
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.data.remote.source.InvoiceRemoteDataSource
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.data.repository.toEntity
import com.fundtrail.di.ApplicationScope
import com.fundtrail.di.IoDispatcher
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore to Room mirror (architecture.md section 4.3). One listener per collection while a user
 * is signed in; each snapshot lands in Room in one transaction. The first snapshot of a run replaces
 * the table, later ones apply changes. A soft-deleted document (`deletedAt` set) is removed like a
 * REMOVED one, because T3 deletes by tombstone and those arrive as MODIFIED.
 */
@Singleton
class SyncManager @Inject constructor(
    private val auth: AuthRepository,
    private val db: FundtrailDatabase,
    private val transactions: TransactionRemoteDataSource,
    private val accounts: AccountRemoteDataSource,
    private val categories: CategoryRemoteDataSource,
    private val invoices: InvoiceRemoteDataSource,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncRepository {

    private val paused = MutableStateFlow(false)
    private val idle = MutableStateFlow(true)

    /** Call once from `Application.onCreate`. Sync follows the signed-in user from then on. */
    fun start() {
        scope.launch(io) {
            // Sign-out is complete once there is no user; the next sign-in must sync again.
            auth.currentUserId.filter { it == null }.collect { paused.value = false }
        }
        scope.launch(io) {
            combine(auth.currentUserId, paused) { uid, isPaused -> uid.takeUnless { isPaused } }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid == null) return@collectLatest
                    idle.value = false
                    try {
                        mirrorAll(uid)
                    } finally {
                        idle.value = true
                    }
                }
        }
    }

    override suspend fun pauseSync() {
        paused.value = true
        idle.first { it } // listeners cancelled and any in-flight Room write finished
    }

    override fun resumeSync() {
        paused.value = false
    }

    private suspend fun mirrorAll(uid: String) = supervisorScope {
        val serverSeen = ConcurrentHashMap.newKeySet<String>()

        suspend fun <D, E : Any> mirror(
            name: String,
            source: Flow<RemoteSnapshot<D>>,
            id: (D) -> String,
            deletedAt: (D) -> Long?,
            toEntity: (D) -> E,
            upsert: suspend (List<E>) -> Unit,
            delete: suspend (List<String>) -> Unit,
            deleteAll: suspend () -> Unit,
        ) {
            var first = true
            source
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Sync listener for $name failed; Room keeps its last state", e)
                }
                .collect { snapshot ->
                    try {
                        val replace = first
                        first = false
                        val (gone, live) = snapshot.changes.partition {
                            it.type == ChangeType.REMOVED || deletedAt(it.item) != null
                        }
                        db.withTransaction {
                            if (replace) {
                                deleteAll()
                            } else {
                                gone.map { id(it.item) }.chunked(SQLITE_ARGS).forEach { delete(it) }
                            }
                            if (live.isNotEmpty()) upsert(live.map { toEntity(it.item) })
                        }
                        if (!snapshot.isFromCache && serverSeen.add(name) && serverSeen.size == COLLECTIONS) {
                            db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Applying $name snapshot failed; Room keeps its last state", e)
                    }
                }
        }

        launch {
            val dao = db.transactionDao()
            mirror(
                "transactions", transactions.observeTransactions(uid),
                Transaction::id, Transaction::deletedAt, Transaction::toEntity,
                dao::upsertAll, dao::deleteByIds, dao::deleteAll,
            )
        }
        launch {
            val dao = db.accountDao()
            mirror(
                "accounts", accounts.observeAccounts(uid),
                Account::id, Account::deletedAt, Account::toEntity,
                dao::upsertAll, dao::deleteByIds, dao::deleteAll,
            )
        }
        launch {
            val dao = db.categoryDao()
            mirror(
                "categories", categories.observeCategories(uid),
                Category::id, Category::deletedAt, Category::toEntity,
                dao::upsertAll, dao::deleteByIds, dao::deleteAll,
            )
        }
        launch {
            val dao = db.invoiceDao()
            mirror(
                "invoices", invoices.observeInvoices(uid),
                Invoice::id, Invoice::deletedAt, Invoice::toEntity,
                dao::upsertAll, dao::deleteByIds, dao::deleteAll,
            )
        }
    }

    private companion object {
        const val TAG = "SyncManager"
        const val COLLECTIONS = 4
        const val SQLITE_ARGS = 900 // SQLite caps bound arguments at 999
    }
}
