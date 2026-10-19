# Quick Add UI
UI components for the Quick Add feature.

`QuickAddRoute(editTransactionId = …)` opens the sheet pre-filled on an existing entry (SCR-11
History row tap, #109): the type tabs lock, Save goes through `UpdateTransactionUseCase` instead
of the `Add*UseCase`s, and no Undo is offered (History offers Undo for its own delete instead).