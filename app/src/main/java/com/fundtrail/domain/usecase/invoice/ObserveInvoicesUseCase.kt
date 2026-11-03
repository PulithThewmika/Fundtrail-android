package com.fundtrail.domain.usecase.invoice

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes all freelance invoices.
 */
class ObserveInvoicesUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    operator fun invoke(): Flow<List<Invoice>> = invoiceRepository.observeAll()
}
