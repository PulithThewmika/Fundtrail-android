package com.fundtrail.domain.usecase.invoice

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeInvoiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InvoiceUseCasesTest {

    private val repository = FakeInvoiceRepository()
    private val observeInvoices = ObserveInvoicesUseCase(repository)
    private val addInvoice = AddInvoiceUseCase(repository)
    private val updateInvoice = UpdateInvoiceUseCase(repository)
    private val markInvoiceReceived = MarkInvoiceReceivedUseCase(repository)
    private val deleteInvoice = DeleteInvoiceUseCase(repository)

    private fun testInvoice(
        id: String = "test_inv_01",
        client: String = "Test Client",
        project: String = "Test Project",
        amountMinor: Long = 5_000_000L,
        currency: String = "LKR",
        dueDate: LocalDate = LocalDate.of(2026, 10, 20),
        status: InvoiceStatus = InvoiceStatus.PENDING
    ) = Invoice(
        id = id,
        client = client,
        project = project,
        amountMinor = amountMinor,
        currency = currency,
        dueDate = dueDate,
        status = status
    )

    @Test
    fun `observeInvoices emits all invoices from repository`() = runTest {
        val invoices = observeInvoices().first()
        assertTrue(invoices.isNotEmpty())
    }

    @Test
    fun `addInvoice adds invoice to repository`() = runTest {
        val invoice = testInvoice(id = "new_inv_123")
        val result = addInvoice(invoice)
        assertTrue(result.isSuccess)
        val all = observeInvoices().first()
        assertTrue(all.any { it.id == "new_inv_123" })
    }

    @Test
    fun `updateInvoice updates existing invoice`() = runTest {
        val invoice = testInvoice(id = "upd_inv_123", client = "Before")
        addInvoice(invoice)
        val updated = invoice.copy(client = "After")
        val result = updateInvoice(updated)
        assertTrue(result.isSuccess)
        val all = observeInvoices().first()
        val found = all.find { it.id == "upd_inv_123" }
        assertNotNull(found)
        assertEquals("After", found?.client)
    }

    @Test
    fun `deleteInvoice deletes invoice`() = runTest {
        val invoice = testInvoice(id = "del_inv_123")
        addInvoice(invoice)
        val result = deleteInvoice("del_inv_123")
        assertTrue(result.isSuccess)
        val all = observeInvoices().first()
        assertTrue(all.none { it.id == "del_inv_123" })
    }

    @Test
    fun `markInvoiceReceived marks invoice RECEIVED and stores settling transaction`() = runTest {
        val invoice = testInvoice(id = "mark_inv_123")
        addInvoice(invoice)

        val settlingTx = Transaction(
            id = "settle_tx_01",
            type = TransactionType.INCOME,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 5_000_000L,
            accountId = "acc_main_bank",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = System.currentTimeMillis()
        )

        val result = markInvoiceReceived("mark_inv_123", settlingTx)
        assertTrue(result.isSuccess)

        val all = observeInvoices().first()
        val found = all.find { it.id == "mark_inv_123" }
        assertNotNull(found)
        assertEquals(InvoiceStatus.RECEIVED, found?.status)
        assertEquals("settle_tx_01", found?.receivedTransactionId)
        assertTrue(repository.recordedTransactions.any { it.id == "settle_tx_01" && it.invoiceId == "mark_inv_123" })
    }
}
