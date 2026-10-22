package com.fundtrail.data.sync

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.local.SyncStateEntity
import com.fundtrail.data.remote.dto.UserProfileDto
import com.fundtrail.data.remote.model.ChangeType
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.data.remote.source.GoalRemoteDataSource
import com.fundtrail.data.remote.source.InvoiceRemoteDataSource
import com.fundtrail.data.remote.source.RecurringRemoteDataSource
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.data.remote.source.UserProfileRemoteDataSource
import com.fundtrail.data.repository.toEntity
import com.fundtrail.di.ApplicationScope
import com.fundtrail.di.IoDispatcher
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.SyncRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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
    private val goals: GoalRemoteDataSource,
    private val recurring: RecurringRemoteDataSource,
    private val profile: UserProfileRemoteDataSource,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncRepository {

    /**
     * The uid sync is paused for, or null when nothing is paused. Pausing is tied to one signed-in
     * user on purpose: `currentUserId` is a conflating flow, so a sign-out followed quickly by a
     * sign-in can hide the intervening null. A bare "paused" flag reset on that null would then stay
     * set and sync would never restart; a different uid is simply never the paused one.
     *
     * Exposed read-only so a test can wait for a sign-out to have been observed before signing
     * back in; conflation makes that unobservable from the auth flow alone.
     */
    @VisibleForTesting
    internal val pausedForUid = MutableStateFlow<String?>(null)
    private val idle = MutableStateFlow(true)

    /** Call once from `Application.onCreate`. Sync follows the signed-in user from then on. */
    fun start() {
        scope.launch(io) {
            combine(auth.currentUserId, pausedForUid) { uid, pausedUid -> uid to pausedUid }
                .distinctUntilChanged()
                .collectLatest { (uid, pausedUid) ->
                    if (uid == null) {
                        // Sign-out is complete; the next sign-in must sync again, same user or not.
                        if (pausedUid != null) pausedForUid.value = null
                        return@collectLatest
                    }
                    if (uid == pausedUid) return@collectLatest
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
        pausedForUid.value = auth.currentUserId.first()
        idle.first { it } // listeners cancelled and any in-flight Room write finished
    }

    override fun resumeSync() {
        pausedForUid.value = null
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
                { incoming ->
                    val existingById = incoming.mapNotNull { entity ->
                        dao.getById(entity.id)?.let { entity.id to it }
                    }.toMap()
                    dao.upsertAll(
                        incoming.map { entity ->
                            val existing = existingById[entity.id]
                            entity.copy(
                                lastNotifiedAt = listOfNotNull(
                                    existing?.lastNotifiedAt,
                                    entity.lastNotifiedAt,
                                ).maxOrNull()
                            )
                        }
                    )
                },
                dao::deleteByIds, dao::deleteAll,
            )
        }
        launch {
            val dao = db.goalDao()
            mirror(
                "goals", goals.observeGoals(uid),
                Goal::id, { null }, Goal::toEntity,
                dao::upsertAll, dao::deleteByIds, dao::deleteAll,
            )
        }
        launch {
            val dao = db.recurringDao()
            mirror(
                "recurring", recurring.observeRecurring(uid),
                RecurringTemplate::id, RecurringTemplate::deletedAt, RecurringTemplate::toEntity,
                dao::upsertAll, dao::deleteByIds, dao::deleteAll,
            )
        }
        launch {
            val dao = db.userSettingsDao()
            // A single-document "collection": the dto carries no stable id, and a missing
            // document arrives as an empty snapshot rather than a REMOVED change.
            mirror(
                "profile", profile.observeProfile(uid),
                { PROFILE_ROW_ID }, { it.deletedAt?.toDate()?.time }, UserProfileDto::toEntity,
                { entities -> entities.lastOrNull()?.let { dao.upsert(it) } }, { dao.deleteAll() }, dao::deleteAll,
            )
        }
    }

    private companion object {
        const val TAG = "SyncManager"
        const val COLLECTIONS = 7
        const val SQLITE_ARGS = 900 // SQLite caps bound arguments at 999
        const val PROFILE_ROW_ID = "profile"
    }
}
