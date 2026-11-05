package com.fundtrail.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.usecase.export.BuildTransactionsCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportCsvState(
    val isLoading: Boolean = false,
    /** One-off: the UI writes this to a file and shares it, then calls [ExportCsvViewModel.onHandled]. */
    val csv: String? = null,
)

/** FR-116: export every transaction as CSV. */
@HiltViewModel
class ExportCsvViewModel @Inject constructor(
    private val buildCsv: BuildTransactionsCsvUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportCsvState())
    val uiState: StateFlow<ExportCsvState> = _uiState.asStateFlow()

    fun onExportClick() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val csv = buildCsv()
            _uiState.update { it.copy(isLoading = false, csv = csv) }
        }
    }

    fun onHandled() = _uiState.update { it.copy(csv = null) }
}
