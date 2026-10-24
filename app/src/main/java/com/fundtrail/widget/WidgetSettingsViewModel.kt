package com.fundtrail.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the look being edited on the widget settings screen. Nothing is stored until [save], so leaving the screen
 * keeps the old look.
 */
@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val repository: WidgetStyleRepository,
    private val updater: WidgetUpdater,
) : ViewModel() {

    private val _style = MutableStateFlow(WidgetStyle())
    val style: StateFlow<WidgetStyle> = _style

    private val _loaded = MutableStateFlow(false)

    /** False until the saved look has been read, so the sliders do not jump from the default. */
    val loaded: StateFlow<Boolean> = _loaded

    init {
        viewModelScope.launch {
            _style.value = repository.current()
            _loaded.value = true
        }
    }

    fun onOpacityChange(percent: Int) = _style.update { it.copy(backgroundPercent = percent).coerced() }

    fun onCornerChange(dp: Int) = _style.update { it.copy(cornerRadiusDp = dp).coerced() }

    fun onTextSizeChange(size: WidgetTextSize) = _style.update { it.copy(textSize = size) }

    fun onReset() { _style.value = WidgetStyle() }

    /**
     * Stores the look, then calls [onSaved]. The widgets redraw in the background (and also because the saved look
     * changed), so closing the screen never waits for the home screen.
     */
    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.save(_style.value)
            updater.refreshInBackground()
            onSaved()
        }
    }
}
