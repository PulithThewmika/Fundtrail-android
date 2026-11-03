package com.fundtrail.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
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

    private val _saveFailed = MutableStateFlow(false)

    /** True when the last [save] could not write to the store, so the screen can say the look was not kept. */
    val saveFailed: StateFlow<Boolean> = _saveFailed

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
     *
     * A failed write would otherwise reach the coroutine handler and take the process down, so it sets
     * [saveFailed] and keeps the screen open with the chosen look still on it.
     */
    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                repository.save(_style.value)
            } catch (_: IOException) {
                _saveFailed.value = true
                return@launch
            }
            updater.refreshInBackground()
            onSaved()
        }
    }
}
