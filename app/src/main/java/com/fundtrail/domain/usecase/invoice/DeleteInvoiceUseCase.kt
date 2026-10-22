package com.fundtrail.domain.usecase.invoice

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.InvoiceRepository
import javax.inject.Inject

/**
 * Deletes a freelance invoice.
 */
class DeleteInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = invoiceRepository.delete(id)
}
