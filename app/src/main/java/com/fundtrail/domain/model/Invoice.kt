package com.fundtrail.domain.model

import java.time.LocalDate

/** `PENDING` until settled; overdue is derived, never stored (see [Invoice.isOverdue]). */
enum class InvoiceStatus { PENDING, RECEIVED }

/**
 * Domain model representing a freelance invoice.
 *
 * @property id Unique invoice ID
 * @property client Client name
 * @property project Project name
 * @property milestone Optional milestone/phase name
 * @property amountMinor Invoiced amount in [currency] minor units
 * @property currency Invoice currency, one of [SUPPORTED_CURRENCIES]
 * @property dueDate Date the invoice is due
 * @property status [InvoiceStatus.PENDING] or [InvoiceStatus.RECEIVED]
 * @property receivedTransactionId Set when [status] is RECEIVED; the settling INCOME transaction
 * @property receivedAt Epoch timestamp (milliseconds) when marked received
 * @property lastNotifiedAt Epoch timestamp (milliseconds) of the last overdue reminder
 * @property note Optional memo
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 * @property issuedAt Epoch timestamp (milliseconds) the invoice was issued (schema.md 3.5). Null on a new
 *   invoice means "now": the remote source stamps it with the server time on first write.
 */
data class Invoice(
    val id: String,
    val client: String,
    val project: String,
    val milestone: String? = null,
    val amountMinor: Long,
    val currency: String,
    val dueDate: LocalDate,
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val receivedTransactionId: String? = null,
    val receivedAt: Long? = null,
    val lastNotifiedAt: Long? = null,
    val note: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
    val issuedAt: Long? = null
) {
    /**
     * Overdue is derived, never stored: [status] is still PENDING and [today] is after [dueDate].
     * The due date itself is not overdue.
     */
    fun isOverdue(today: LocalDate): Boolean = status == InvoiceStatus.PENDING && today.isAfter(dueDate)

    /**
     * Validates invoice invariants:
     * - [id], [client], [project] must not be blank.
     * - [currency] must be one of [SUPPORTED_CURRENCIES].
     * - [amountMinor] must be > 0.
     * - RECEIVED requires [receivedTransactionId] (non-blank).
     */
    fun validate() {
        require(id.isNotBlank()) { "Invoice ID must not be blank" }
        require(client.isNotBlank()) { "Invoice client must not be blank" }
        require(project.isNotBlank()) { "Invoice project must not be blank" }
        require(currency in SUPPORTED_CURRENCIES) { "Unsupported invoice currency: $currency" }
        require(amountMinor > 0) { "Invoice amount must be greater than zero" }
        if (status == InvoiceStatus.RECEIVED) {
            require(!receivedTransactionId.isNullOrBlank()) {
                "A received invoice must reference its settling transaction"
            }
        }
    }
}
