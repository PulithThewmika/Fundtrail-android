package com.fundtrail.domain.usecase.invoice

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.InvoiceRepository
import javax.inject.Inject

/**
 * Updates an existing freelance invoice. Validation lives in the repository, which calls
 * [Invoice.validate] before it writes.
 */
class UpdateInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(invoice: Invoice): Result<Unit> = invoiceRepository.update(invoice)
}
