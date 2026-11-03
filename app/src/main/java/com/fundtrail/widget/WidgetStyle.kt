package com.fundtrail.widget

/** How big the widget's text is. [scale] multiplies the normal sizes. */
enum class WidgetTextSize(val scale: Float) {
    SMALL(0.85f),
    MEDIUM(1f),
    LARGE(1.2f),
}

/**
 * The look the user chose for the home-screen widget: how see-through the background is, how round the corners
 * are, and how big the text is. One look is shared by every FundTrail widget on the device.
 *
 * @property backgroundPercent Background opacity, 60 (see-through) to 100 (solid).
 * @property cornerRadiusDp Corner roundness, 0 (square) to 32.
 */
data class WidgetStyle(
    val backgroundPercent: Int = DEFAULT_BACKGROUND_PERCENT,
    val cornerRadiusDp: Int = DEFAULT_CORNER_RADIUS_DP,
    val textSize: WidgetTextSize = WidgetTextSize.MEDIUM,
) {
    val backgroundAlpha: Float get() = backgroundPercent / 100f

    /** The same look with every value inside its allowed range, so a bad stored value can never break the widget. */
    fun coerced() = copy(
        backgroundPercent = backgroundPercent.coerceIn(MIN_BACKGROUND_PERCENT, MAX_BACKGROUND_PERCENT),
        cornerRadiusDp = cornerRadiusDp.coerceIn(0, MAX_CORNER_RADIUS_DP),
    )

    companion object {
        // The wallpaper shows through the rest, and the text colours are fixed theme roles, so a low floor can put
        // the text under the 4.5:1 NFR-15 needs. 60 removes the worst of that; the pathological case (a white
        // wallpaper behind dark-theme text) still needs a solid panel behind the text.
        // TODO(M4): draw the text on a solid surface and keep only the frame translucent, for 4.5:1 everywhere.
        const val MIN_BACKGROUND_PERCENT = 60
        const val MAX_BACKGROUND_PERCENT = 100
        const val MAX_CORNER_RADIUS_DP = 32

        /** The original look: solid background, 16 dp corners, normal text. */
        const val DEFAULT_BACKGROUND_PERCENT = 100
        const val DEFAULT_CORNER_RADIUS_DP = 16
    }
}
