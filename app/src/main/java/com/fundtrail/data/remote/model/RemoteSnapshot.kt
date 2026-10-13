package com.fundtrail.data.remote.model

import com.google.firebase.firestore.DocumentChange

/**
 * Encapsulates incremental remote document changes and cache status for sync.
 */
data class RemoteChange<T>(
    val type: DocumentChange.Type,
    val item: T
)

data class RemoteSnapshot<T>(
    val changes: List<RemoteChange<T>>,
    val isFromCache: Boolean
)
