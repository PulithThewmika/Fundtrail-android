# Data Sync

Firestore to Room mirror (architecture.md section 4.3, ADR-0004).

- `SyncManager` runs in the `@ApplicationScope` scope on the injected `@IoDispatcher`. `FundtrailApplication` calls `start()`; it then follows `AuthRepository.currentUserId`.
- One listener each for transactions, accounts and categories. Every snapshot is applied to Room in one transaction: the first snapshot of a run replaces the table, later ones upsert ADDED/MODIFIED and delete REMOVED.
- A document with `deletedAt` set (soft delete) is deleted from Room even though Firestore reports it as MODIFIED.
- `sync_state.initialSyncDone` is set once all three collections have delivered a server (non-cache) snapshot.
- A failing listener is logged and left stopped; the other two keep running and Room keeps its last state.
- `SyncRepository.pauseSync()` is called by `SignOutUseCase` before auth goes away. It returns only after no Room write is in flight, so `clearLocalData()` cannot be overwritten by a late snapshot. `resumeSync()` is used when sign-out fails; after a successful sign-out sync resumes by itself on the next sign-in.
