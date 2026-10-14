package com.fundtrail.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.domain.model.SyncStatus
import com.fundtrail.domain.repository.SyncStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class SyncStatusRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateDao: SyncStateDao,
) : SyncStatusRepository {

    override fun observe(): Flow<SyncStatus> =
        combine(syncStateDao.observe(), isOnline()) { state, online ->
            SyncStatus(initialSyncDone = state?.initialSyncDone == true, isOnline = online)
        }

    private fun isOnline(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        fun NetworkCapabilities?.validated() = this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.validated())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(cm.getNetworkCapabilities(cm.activeNetwork).validated())
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
