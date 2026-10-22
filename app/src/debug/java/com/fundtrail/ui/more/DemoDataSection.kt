package com.fundtrail.ui.more

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.debug.demo.ClearDemoDataUseCase
import com.fundtrail.debug.demo.SeedDemoDataUseCase
import com.fundtrail.domain.model.Result
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Last outcome of a seed or clear: how many documents were written ([seeded]) or removed ([cleared]). */
data class DemoDataUiState(
    val isRunning: Boolean = false,
    val seeded: Int? = null,
    val cleared: Int? = null,
    @StringRes val error: Int? = null,
)

/** Debug-only demo data tools on More (#157). The release source set has a no-op [DemoDataSection]. */
@HiltViewModel
class DemoDataViewModel @Inject constructor(
    private val seedDemoData: SeedDemoDataUseCase,
    private val clearDemoData: ClearDemoDataUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DemoDataUiState())
    val uiState: StateFlow<DemoDataUiState> = _uiState.asStateFlow()

    fun onSeed() = run({ seedDemoData() }) { copy(seeded = it) }

    fun onClear() = run({ clearDemoData() }) { copy(cleared = it) }

    private fun run(action: suspend () -> Result<Int>, done: DemoDataUiState.(Int) -> DemoDataUiState) {
        if (_uiState.value.isRunning) return
        _uiState.value = DemoDataUiState(isRunning = true)
        viewModelScope.launch {
            _uiState.value = when (val result = action()) {
                is Result.Success -> DemoDataUiState().done(result.data)
                is Result.Error -> DemoDataUiState(error = mapErrorToMessage(result.exception))
            }
        }
    }
}

@Composable
fun DemoDataSection(viewModel: DemoDataViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DemoDataContent(state, onSeed = viewModel::onSeed, onClear = viewModel::onClear)
}

@Composable
private fun DemoDataContent(state: DemoDataUiState, onSeed: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.demo_data_title), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onSeed, enabled = !state.isRunning, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.demo_data_seed))
            }
            OutlinedButton(onClick = onClear, enabled = !state.isRunning, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.demo_data_clear))
            }
        }
        val message = when {
            state.isRunning -> stringResource(R.string.demo_data_running)
            state.error != null -> stringResource(state.error)
            state.seeded != null -> pluralStringResource(R.plurals.demo_data_seeded, state.seeded, state.seeded)
            state.cleared != null -> pluralStringResource(R.plurals.demo_data_cleared, state.cleared, state.cleared)
            else -> null
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
