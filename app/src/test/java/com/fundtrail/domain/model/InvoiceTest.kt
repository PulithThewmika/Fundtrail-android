package com.fundtrail.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InvoiceTest {

    private fun invoice(
        dueDate: LocalDate = LocalDate.of(2026, 10, 20),
        status: InvoiceStatus = InvoiceStatus.PENDING
    ) = Invoice(
        id = "inv_01",
        client = "Acme Co",
        project = "Landing page",
        amountMinor = 5_000_000L,
        currency = "LKR",
        dueDate = dueDate,
        status = status
    )

    @Test
    fun `due date itself is not overdue`() {
        val inv = invoice(dueDate = LocalDate.of(2026, 10, 20))
        assertFalse(inv.isOverdue(today = LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun `day after due date is overdue`() {
        val inv = invoice(dueDate = LocalDate.of(2026, 10, 20))
        assertTrue(inv.isOverdue(today = LocalDate.of(2026, 10, 21)))
    }

    @Test
    fun `day before due date is not overdue`() {
        val inv = invoice(dueDate = LocalDate.of(2026, 10, 20))
        assertFalse(inv.isOverdue(today = LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun `received invoice past due date is never overdue`() {
        val inv = invoice(dueDate = LocalDate.of(2026, 10, 20), status = InvoiceStatus.RECEIVED)
            .copy(receivedTransactionId = "tx_01")
        assertFalse(inv.isOverdue(today = LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `valid invoice succeeds validation`() {
        invoice().validate()
        assertTrue(true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported currency fails validation`() {
        invoice().copy(currency = "EUR").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero amount fails validation`() {
        invoice().copy(amountMinor = 0L).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `received without transaction id fails validation`() {
        invoice(status = InvoiceStatus.RECEIVED).validate()
    }
}
