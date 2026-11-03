package com.fundtrail.domain.usecase.invoice

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.repository.InvoiceRepository
import javax.inject.Inject

/**
 * Marks an invoice as received and records the settling INCOME transaction in one atomic batch.
 */
class MarkInvoiceReceivedUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(id: String, transaction: Transaction): Result<Unit> =
        invoiceRepository.markReceived(id, transaction)
}
