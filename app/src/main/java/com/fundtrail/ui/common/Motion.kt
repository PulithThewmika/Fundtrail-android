package com.fundtrail.ui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * The app's motion vocabulary. Short and quiet: nothing runs longer than [SLOW], and every effect shows where
 * something came from or that something changed. With system animations off, [pressScale] and [Haptics] do nothing.
 */
object Motion {
    const val FAST = 150
    const val STANDARD = 220
    const val SLOW = 300

    /** Quick start, soft landing: the standard easing for things that arrive. */
    val Decelerate = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** A light tick for a change of selection, a firm confirm for something that was saved. */
class Haptics internal constructor(private val view: View, private val enabled: Boolean) {
    fun tick() {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun success() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = systemAnimationsEnabled()
    return remember(view, enabled) { Haptics(view, enabled) }
}

/** Presses the element down slightly while a finger is on it, so taps feel physical. [source] must drive the click. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.98f): Modifier {
    val animate = systemAnimationsEnabled()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animate) pressedScale else 1f,
        animationSpec = tween(if (pressed) 100 else Motion.FAST),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
