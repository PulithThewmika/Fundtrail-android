package com.fundtrail.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The standard Material light and dark surface colours the see-through background is built from. */
internal const val LIGHT_SURFACE = 0xFFFFFBFEL
internal const val DARK_SURFACE = 0xFF1C1B1FL

/** Longest side of the drawn background, so the picture sent to the home screen stays small. */
private const val MAX_SIDE_PX = 800f

/**
 * What to draw for the widget's background on Android 8-11. Glance 1.2 cannot round corners before Android 12,
 * so there the background is drawn here as a picture instead. Everything is in pixels.
 */
internal data class BackgroundSpec(val widthPx: Int, val heightPx: Int, val radiusPx: Float, val argb: Int)

/**
 * Works out the picture for a widget of [widthDp] x [heightDp]. A very large widget is drawn smaller and stretched
 * evenly, so the corners keep their shape. The opacity goes into the colour's alpha.
 */
internal fun backgroundSpec(
    widthDp: Float,
    heightDp: Float,
    density: Float,
    style: WidgetStyle,
    night: Boolean,
): BackgroundSpec {
    val rawWidth = max(1f, widthDp * density)
    val rawHeight = max(1f, heightDp * density)
    val shrink = min(1f, MAX_SIDE_PX / max(rawWidth, rawHeight))
    val surface = if (night) DARK_SURFACE else LIGHT_SURFACE
    val alpha = (style.backgroundAlpha * 255).roundToInt().coerceIn(0, 255)
    return BackgroundSpec(
        widthPx = (rawWidth * shrink).roundToInt().coerceAtLeast(1),
        heightPx = (rawHeight * shrink).roundToInt().coerceAtLeast(1),
        radiusPx = style.cornerRadiusDp * density * shrink,
        argb = (alpha shl 24) or (surface.toInt() and 0x00FFFFFF),
    )
}

internal fun BackgroundSpec.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb }
    // A radius larger than half the side would be clamped by the canvas anyway; keep it sane for the maths above.
    val radius = min(radiusPx, min(widthPx, heightPx) / 2f)
    Canvas(bitmap).drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radius, radius, paint)
    return bitmap
}
