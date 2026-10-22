package com.fundtrail.ui.income

import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.SyncStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.income.ObserveCryptoYtdUseCase
import com.fundtrail.domain.usecase.income.ObserveSalaryStatusUseCase
import com.fundtrail.domain.usecase.invoice.AddInvoiceUseCase
import com.fundtrail.domain.usecase.invoice.MarkInvoiceReceivedUseCase
import com.fundtrail.domain.usecase.invoice.ObserveInvoicesUseCase
import com.fundtrail.domain.usecase.invoice.UpdateInvoiceUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.domain.usecase.transaction.ObserveTransactionByIdUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeInvoiceRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeSyncStatusRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class IncomeViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val invoiceRepo = FakeInvoiceRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val accountRepo = FakeAccountRepository()
    private val syncRepo = FakeSyncStatusRepository()

    // 07/10/2026 as per S4 wireframe
    private val fixedToday = LocalDate.of(2026, 10, 7)
    private val fixedClock = Clock.fixed(fixedToday.atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)

    private fun TestScope.viewModel() = IncomeViewModel(
        observeInvoices = ObserveInvoicesUseCase(invoiceRepo),
        observeSalaryStatus = ObserveSalaryStatusUseCase(settingsRepo, transactionRepo, fixedClock),
        observeCryptoYtd = ObserveCryptoYtdUseCase(transactionRepo, fixedClock),
        observeAccounts = ObserveAccountsUseCase(accountRepo),
        observeSyncStatus = ObserveSyncStatusUseCase(syncRepo),
        addInvoice = AddInvoiceUseCase(invoiceRepo),
        updateInvoice = UpdateInvoiceUseCase(invoiceRepo),
        markInvoiceReceived = MarkInvoiceReceivedUseCase(invoiceRepo),
        observeTransactionById = ObserveTransactionByIdUseCase(transactionRepo),
        clock = fixedClock
    ).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    private fun IncomeViewModel.data() = (uiState.value.content as UiState.Success).data

    @Test
    fun `groups invoices into Overdue, Pending, Received with Overdue first`() = runTest {
        // Clear default sample invoices and add specific ones
        val all = invoiceRepo.observeAll().first()
        all.forEach { invoiceRepo.delete(it.id) }

        // Overdue invoice (due 01/10/2026, today 07/10/2026 -> 6 days overdue)
        val overdueInv = Invoice(
            id = "inv_overdue",
            client = "Nova Cafe",
            project = "Logo design",
            milestone = "Milestone 2",
            amountMinor = 3_000_000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 10, 1),
            status = InvoiceStatus.PENDING
        )

        // Pending invoice (due 12/10/2026 -> 5 days in future)
        val pendingInv = Invoice(
            id = "inv_pending",
            client = "Ceylon Tours",
            project = "Web app",
            milestone = "Milestone 2",
            amountMinor = 4_500_000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 10, 12),
            status = InvoiceStatus.PENDING
        )

        // Received invoice
        val receivedInv = Invoice(
            id = "inv_received",
            client = "Brand guide",
            project = "Milestone 1",
            amountMinor = 2_000_000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 9, 15),
            status = InvoiceStatus.RECEIVED,
            receivedTransactionId = "tx_settled_123"
        )

        invoiceRepo.add(overdueInv)
        invoiceRepo.add(pendingInv)
        invoiceRepo.add(receivedInv)

        val vm = viewModel()
        val data = vm.data()

        // Overdue sits at the top
        assertEquals(1, data.overdueGroup.count)
        assertEquals("LKR 30,000.00", data.overdueGroup.totalFormatted)
        val overdueItem = data.overdueGroup.invoices.first()
        assertEquals("inv_overdue", overdueItem.invoice.id)
        assertEquals(6L, overdueItem.daysOverdue)

        // Pending
        assertEquals(1, data.pendingGroup.count)
        assertEquals("LKR 45,000.00", data.pendingGroup.totalFormatted)
        val pendingItem = data.pendingGroup.invoices.first()
        assertEquals("inv_pending", pendingItem.invoice.id)
        assertNull(pendingItem.daysOverdue)

        // Received
        assertEquals(1, data.receivedGroup.count)
        assertEquals("LKR 20,000.00", data.receivedGroup.totalFormatted)
        val receivedItem = data.receivedGroup.invoices.first()
        assertEquals("inv_received", receivedItem.invoice.id)
        assertEquals("tx_settled_123", receivedItem.invoice.receivedTransactionId)
    }

    @Test
    fun `shows foreign currency invoices clearly in totals`() = runTest {
        val all = invoiceRepo.observeAll().first()
        all.forEach { invoiceRepo.delete(it.id) }

        val invLkr = Invoice(
            id = "inv_lkr",
            client = "Client A",
            project = "Project A",
            amountMinor = 5_000_000L, // LKR 50,000
            currency = "LKR",
            dueDate = LocalDate.of(2026, 10, 20),
            status = InvoiceStatus.PENDING
        )

        val invUsd = Invoice(
            id = "inv_usd",
            client = "Client B",
            project = "Project B",
            amountMinor = 30_000L, // USD 300
            currency = "USD",
            dueDate = LocalDate.of(2026, 10, 25),
            status = InvoiceStatus.PENDING
        )

        invoiceRepo.add(invLkr)
        invoiceRepo.add(invUsd)

        val vm = viewModel()
        val pending = vm.data().pendingGroup
        assertEquals(2, pending.count)
        assertEquals("LKR 50,000.00 + USD 300.00", pending.totalFormatted)
    }

    @Test
    fun `salary card reflects expected and received amounts with variance`() = runTest {
        settingsRepo.update(
            UserSettings(
                expectedSalaryMinor = 13_000_000L, // 130,000 LKR
                salaryPayday = 25
            )
        )

        val vm = viewModel()
        val salary = vm.data().salary
        assertTrue(salary.isReceived)
        assertEquals(13_000_000L, salary.expectedAmountMinor)
        assertEquals(13_000_000L, salary.receivedAmountMinor)
        assertEquals(0L, salary.varianceMinor)
    }

    @Test
    fun `crypto YTD card reflects net position`() = runTest {
        val vm = viewModel()
        val crypto = vm.data().cryptoYtd
        assertEquals(2026, crypto.year)
        // From sample transactions in Oct 2026: -372_124L
        assertEquals(-372_124L, crypto.netLkrMinor)
    }

    @Test
    fun `add invoice form validates and adds new invoice`() = runTest {
        val vm = viewModel()
        vm.onAddInvoiceClick()

        assertNotNull(vm.uiState.value.invoiceForm)
        vm.onFormClientChange("Acme Corp")
        vm.onFormProjectChange("Website Redesign")
        vm.onFormMilestoneChange("Phase 1")
        vm.onFormAmountChange("75000")
        vm.onFormCurrencyChange("LKR")
        vm.onFormDueDateChange(LocalDate.of(2026, 11, 15))
        vm.onFormNoteChange("Payment on delivery")

        vm.onFormSave()

        assertNull(vm.uiState.value.invoiceForm)
        assertEquals(R.string.income_invoice_saved, vm.uiState.value.message)

        val all = invoiceRepo.observeAll().first()
        val added = all.find { it.client == "Acme Corp" }
        assertNotNull(added)
        assertEquals("Website Redesign", added?.project)
        assertEquals(7_500_000L, added?.amountMinor)
    }

    @Test
    fun `add invoice form rejects blank client or project or zero amount`() = runTest {
        val vm = viewModel()
        vm.onAddInvoiceClick()

        vm.onFormClientChange("")
        vm.onFormProjectChange("")
        vm.onFormAmountChange("0")
        vm.onFormSave()

        val form = vm.uiState.value.invoiceForm
        assertNotNull(form)
        assertEquals(R.string.income_client_required, form?.clientError)
        assertEquals(R.string.income_project_required, form?.projectError)
        assertEquals(R.string.income_amount_required, form?.amountError)
    }

    @Test
    fun `edit invoice form pre-populates and updates invoice`() = runTest {
        val inv = Invoice(
            id = "edit_inv_01",
            client = "Before Edit",
            project = "App",
            amountMinor = 1_000_000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 10, 20),
            status = InvoiceStatus.PENDING
        )
        invoiceRepo.add(inv)

        val vm = viewModel()
        vm.onEditInvoiceClick(inv)

        val form = vm.uiState.value.invoiceForm
        assertNotNull(form)
        assertEquals("edit_inv_01", form?.id)
        assertEquals("Before Edit", form?.client)

        vm.onFormClientChange("After Edit")
        vm.onFormSave()

        assertNull(vm.uiState.value.invoiceForm)
        val updated = invoiceRepo.observeAll().first().find { it.id == "edit_inv_01" }
        assertEquals("After Edit", updated?.client)
    }

    @Test
    fun `mark received asks for actual amount prefilled and runs atomic batch`() = runTest {
        val account = Account(
            id = "acc_bank_1",
            name = "Commercial Bank",
            kind = AccountKind.BANK,
            currency = "LKR",
            openingBalanceLkrMinor = 0L,
            sortOrder = 1
        )
        accountRepo.add(account)

        val inv = Invoice(
            id = "receive_inv_01",
            client = "Nova Cafe",
            project = "Logo design",
            milestone = "Milestone 2",
            amountMinor = 3_000_000L, // 30,000 LKR
            currency = "LKR",
            dueDate = LocalDate.of(2026, 10, 1),
            status = InvoiceStatus.PENDING
        )
        invoiceRepo.add(inv)

        val vm = viewModel()
        vm.onMarkReceivedClick(inv)

        val dialog = vm.uiState.value.markReceivedDialog
        assertNotNull(dialog)
        // Prefilled with actual amount 30000
        assertEquals("30000", dialog?.actualAmount)
        assertEquals("acc_bank_1", dialog?.selectedAccountId)

        // Confirm receipt
        vm.onMarkReceivedConfirm()

        assertNull(vm.uiState.value.markReceivedDialog)
        assertEquals(R.string.income_invoice_received_success, vm.uiState.value.message)

        val settledInvoice = invoiceRepo.observeAll().first().find { it.id == "receive_inv_01" }
        assertNotNull(settledInvoice)
        assertEquals(InvoiceStatus.RECEIVED, settledInvoice?.status)
        assertNotNull(settledInvoice?.receivedTransactionId)

        // Settling transaction written in atomic batch
        val tx = invoiceRepo.recordedTransactions.find { it.invoiceId == "receive_inv_01" }
        assertNotNull(tx)
        assertEquals(3_000_000L, tx?.amountLkrMinor)
        assertEquals(TransactionType.INCOME, tx?.type)
        assertEquals(TransactionStatus.ACTUAL, tx?.status)
        assertEquals(IncomeSource.FREELANCE, tx?.incomeSource)
        assertEquals("Nova Cafe - Logo design - Milestone 2", tx?.note)
    }

    @Test
    fun `clicking linked transaction opens details dialog`() = runTest {
        val tx = Transaction(
            id = "tx_link_999",
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 2_000_000L,
            accountId = "acc_main_bank",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = fixedToday.atStartOfDay(APP_ZONE).toInstant().toEpochMilli()
        )
        transactionRepo.add(tx)

        val inv = Invoice(
            id = "inv_linked_01",
            client = "Gamma Ltd",
            project = "API integration",
            amountMinor = 2_000_000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 9, 1),
            status = InvoiceStatus.RECEIVED,
            receivedTransactionId = "tx_link_999"
        )
        invoiceRepo.add(inv)

        val vm = viewModel()
        vm.onViewTransactionClick(inv)

        val dialog = vm.uiState.value.viewTransactionDialog
        assertNotNull(dialog)
        assertEquals("tx_link_999", dialog?.transactionId)
        assertEquals(2_000_000L, dialog?.transaction?.amountLkrMinor)

        vm.onViewTransactionDismiss()
        assertNull(vm.uiState.value.viewTransactionDialog)
    }

    @Test
    fun `empty state shows neutral copy when no data exists`() = runTest {
        // Clear all invoices
        invoiceRepo.observeAll().first().forEach { invoiceRepo.delete(it.id) }

        // Clear settings
        settingsRepo.update(UserSettings(expectedSalaryMinor = null, incomeEstimateMinMinor = null, incomeEstimateMaxMinor = null))

        // Set empty sync status
        syncRepo.status.value = SyncStatus(initialSyncDone = true, isOnline = true)

        // Also we need no transactions
        val emptyTxRepo = FakeTransactionRepository()
        // clear sample transactions
        KavinduSampleData.sampleTransactions.forEach { emptyTxRepo.delete(it.id) }

        val vm = IncomeViewModel(
            observeInvoices = ObserveInvoicesUseCase(invoiceRepo),
            observeSalaryStatus = ObserveSalaryStatusUseCase(settingsRepo, emptyTxRepo, fixedClock),
            observeCryptoYtd = ObserveCryptoYtdUseCase(emptyTxRepo, fixedClock),
            observeAccounts = ObserveAccountsUseCase(accountRepo),
            observeSyncStatus = ObserveSyncStatusUseCase(syncRepo),
            addInvoice = AddInvoiceUseCase(invoiceRepo),
            updateInvoice = UpdateInvoiceUseCase(invoiceRepo),
            markInvoiceReceived = MarkInvoiceReceivedUseCase(invoiceRepo),
            observeTransactionById = ObserveTransactionByIdUseCase(emptyTxRepo),
            clock = fixedClock
        ).also { v ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                v.uiState.collect {}
            }
        }

        assertEquals(UiState.Empty, vm.uiState.value.content)
    }

    @Test
    fun `message is cleared after shown`() = runTest {
        val vm = viewModel()
        vm.onAddInvoiceClick()
        vm.onFormClientChange("Client")
        vm.onFormProjectChange("Project")
        vm.onFormAmountChange("1000")
        vm.onFormSave()

        assertNotNull(vm.uiState.value.message)
        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }
}
