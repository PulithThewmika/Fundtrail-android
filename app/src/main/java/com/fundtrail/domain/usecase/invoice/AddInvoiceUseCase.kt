package com.fundtrail.domain.usecase.invoice

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.InvoiceRepository
import javax.inject.Inject

/**
 * Validates and adds a new freelance invoice.
 */
class AddInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(invoice: Invoice): Result<Unit> = invoiceRepository.add(invoice)
}
