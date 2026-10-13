package com.fundtrail.domain.model

/**
 * What the screens need to know about sync (architecture.md §4.5).
 *
 * @property initialSyncDone Every listener has had one snapshot from the server for this user.
 * @property isOnline The device has a validated network connection.
 */
// TODO(M3): add lastError for the non-blocking listener-error snackbar (§4.6).
data class SyncStatus(val initialSyncDone: Boolean, val isOnline: Boolean)
