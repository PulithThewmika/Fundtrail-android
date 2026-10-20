# Data Sync

Firestore to Room mirror (architecture.md section 4.3, ADR-0004).

- `SyncManager` runs in the `@ApplicationScope` scope on the injected `@IoDispatcher`. `FundtrailApplication` calls `start()`; it then follows `AuthRepository.currentUserId`.
- One listener each for transactions, accounts, categories and goals, plus one on the `users/{uid}` profile document (treated as a one-row "collection"). Every snapshot is applied to Room in one transaction: the first snapshot of a run replaces the table, later ones upsert ADDED/MODIFIED and delete REMOVED.
- A document with `deletedAt` set (soft delete) is deleted from Room even though Firestore reports it as MODIFIED. Goals and the profile document never set `deletedAt` today; there's no delete use case for either.
- `sync_state.initialSyncDone` is set once all five collections have delivered a server (non-cache) snapshot.
- A failing listener is logged and left stopped; the others keep running and Room keeps its last state.
- `SyncRepository.pauseSync()` is called by `SignOutUseCase` before auth goes away. It returns only after no Room write is in flight, so `clearLocalData()` cannot be overwritten by a late snapshot. `resumeSync()` is used when sign-out fails; after a successful sign-out sync resumes by itself on the next sign-in.
