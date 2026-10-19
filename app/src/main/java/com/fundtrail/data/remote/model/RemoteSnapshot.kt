package com.fundtrail.data.remote.model

import com.google.firebase.firestore.DocumentChange

/** Firestore-free copy of `DocumentChange.Type`, so `data/sync` never imports Firestore. */
enum class ChangeType { ADDED, MODIFIED, REMOVED }

fun DocumentChange.Type.toChangeType(): ChangeType = when (this) {
    DocumentChange.Type.ADDED -> ChangeType.ADDED
    DocumentChange.Type.MODIFIED -> ChangeType.MODIFIED
    DocumentChange.Type.REMOVED -> ChangeType.REMOVED
}

/**
 * Encapsulates incremental remote document changes and cache status for sync.
 */
data class RemoteChange<T>(
    val type: ChangeType,
    val item: T
)

data class RemoteSnapshot<T>(
    val changes: List<RemoteChange<T>>,
    val isFromCache: Boolean
)
