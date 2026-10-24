package com.fundtrail.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The background picture drawn on Android 8-11, where Glance cannot round corners or tint by itself. */
class WidgetBackgroundTest {

    private fun spec(
        style: WidgetStyle,
        night: Boolean = false,
        widthDp: Float = 180f,
        heightDp: Float = 110f,
        density: Float = 2f,
    ) = backgroundSpec(widthDp, heightDp, density, style, night)

    @Test
    fun `size and corner follow the widget size and the chosen roundness`() {
        val s = spec(WidgetStyle(cornerRadiusDp = 20))
        assertEquals(360, s.widthPx)
        assertEquals(220, s.heightPx)
        assertEquals(40f, s.radiusPx, 0.001f) // 20 dp at 2x
    }

    @Test
    fun `opacity goes into the alpha of the colour`() {
        val half = spec(WidgetStyle(backgroundPercent = 50)).argb
        assertEquals(128, (half ushr 24) and 0xFF) // 50% of 255, rounded
        val solid = spec(WidgetStyle(backgroundPercent = 100)).argb
        assertEquals(255, (solid ushr 24) and 0xFF)
    }

    @Test
    fun `a more see-through setting has a lower alpha`() {
        val faint = spec(WidgetStyle(backgroundPercent = 20)).argb ushr 24
        val strong = spec(WidgetStyle(backgroundPercent = 80)).argb ushr 24
        assertTrue(faint < strong)
    }

    @Test
    fun `light and dark use their own surface colour`() {
        assertEquals(0xFFFBFE, spec(WidgetStyle(), night = false).argb and 0xFFFFFF)
        assertEquals(0x1C1B1F, spec(WidgetStyle(), night = true).argb and 0xFFFFFF)
    }

    @Test
    fun `a square corner has no radius`() {
        assertEquals(0f, spec(WidgetStyle(cornerRadiusDp = 0)).radiusPx, 0f)
    }

    @Test
    fun `a very large widget is drawn smaller, keeping the corner in proportion`() {
        val big = spec(WidgetStyle(cornerRadiusDp = 20), widthDp = 1000f, heightDp = 500f, density = 3f)
        // 3000 x 1500 px would be sent as-is otherwise; it is shrunk so the long side is 800 px.
        assertEquals(800, big.widthPx)
        assertEquals(400, big.heightPx)
        assertEquals(20f * 3f * (800f / 3000f), big.radiusPx, 0.01f)
    }

    @Test
    fun `an unknown zero size never produces an empty picture`() {
        val s = spec(WidgetStyle(), widthDp = 0f, heightDp = 0f)
        assertTrue(s.widthPx >= 1 && s.heightPx >= 1)
    }
}
