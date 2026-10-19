# Offline → online sync verification (#87)

Manual run of the airplane-mode sequence from #87's acceptance criteria, on the Pixel 10 Pro emulator (API 37; #128's full sign-off still needs an API 26 run). Test account: `fundtrail.offline.test@example.com` (created for this run only).

## Steps and evidence

1. **Register a new user.** Sign-up creates the Firebase Auth account and the 4 default accounts / default categories.
   ![Create account](https://github.com/user-attachments/assets/d64a5fc0-9d49-4c96-8b33-37ea95721385)

2. **Airplane mode on.** The "You're offline" banner appears immediately.
   ![Offline banner](https://github.com/user-attachments/assets/60b02aa6-faa7-42f2-a8b0-6af2525a8adc)

3. **Add an expense while offline** (Rent, LKR 1,112.00, Main bank). Saved and visible in the Insights dashboard well under 1 second after tapping Save.
   ![Saved under 1s](https://github.com/user-attachments/assets/d00baa7a-60de-4688-8a61-046442d6d96c)

   It also appears immediately in History:
   ![History offline](https://github.com/user-attachments/assets/989a9970-f5fa-47fa-9809-9a5b98b9b444)

4. **Force-stop the app**, then reopen it **while still offline**. The entry is still there — it was written to Room, not just held in memory.
   ![Reopen offline](https://github.com/user-attachments/assets/127dc101-9301-4f97-adb6-5f0fa10037da)

5. **Reconnect.** The offline banner clears.
   ![Reconnected](https://github.com/user-attachments/assets/dc96ca17-9d4c-4c95-9a3a-331c7e78514c)

6. **Verify the write reached Firestore.** Rather than extract any credential or token to query Firestore directly, this was verified through the app's own flow: Sign out (clears Room per the M1 logout rule — architecture.md, #51) while online, then sign back in with the same account. Room starts empty again, so anything that reappears had to come from Firestore's `users/{uid}/transactions` collection.

   The Rent entry reappears after the fresh sign-in, confirming the pending write synced to Firestore while offline and was pulled back down cleanly:
   ![Synced from Firestore after fresh sign-in](https://github.com/user-attachments/assets/326ca7d0-b1f4-43b3-a742-41a74cf2e913)

## Result

All three AC bullets hold:
- `DocumentChange` / Room write-through: the expense is visible in the dashboard and History within 1s of Save, while offline.
- Force-stop + reopen offline: the entry survives, proving it's in Room, not just view state.
- Reconnect: the entry is present in Firestore (`users/{uid}/transactions`), verified via sign-out/sign-in round-trip rather than a direct console/API query.

Test account `fundtrail.offline.test@example.com` and its data are safe to delete; they exist only in the dev Firebase project for this test run.
