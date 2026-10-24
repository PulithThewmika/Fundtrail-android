package com.fundtrail.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The look the user picks for the home-screen widget (opacity, corners, text size). */
class WidgetStyleRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.dataStore() =
        // A path that does not exist yet: DataStore creates the file itself and cannot replace one made by the test.
        PreferenceDataStoreFactory.create(scope = backgroundScope) { java.io.File(folder.root, "widget.preferences_pb") }

    @Test
    fun `nothing saved gives the original look`() = runTest {
        val style = DataStoreWidgetStyleRepository(dataStore()).observe().first()
        assertEquals(WidgetStyle(backgroundPercent = 100, cornerRadiusDp = 16, textSize = WidgetTextSize.MEDIUM), style)
    }

    @Test
    fun `a saved look is read back`() = runTest {
        val repo = DataStoreWidgetStyleRepository(dataStore())
        val chosen = WidgetStyle(backgroundPercent = 55, cornerRadiusDp = 28, textSize = WidgetTextSize.LARGE)
        repo.save(chosen)
        assertEquals(chosen, repo.observe().first())
    }

    @Test
    fun `saving a look outside the allowed range stores the nearest allowed one`() = runTest {
        val repo = DataStoreWidgetStyleRepository(dataStore())
        repo.save(WidgetStyle(backgroundPercent = 0, cornerRadiusDp = 500))
        assertEquals(WidgetStyle(backgroundPercent = 20, cornerRadiusDp = 32), repo.observe().first())
    }

    @Test
    fun `bad stored values never break the widget`() = runTest {
        val store = dataStore()
        store.edit {
            it[intPreferencesKey("widget_background_percent")] = -40
            it[intPreferencesKey("widget_corner_radius_dp")] = 9_999
            it[stringPreferencesKey("widget_text_size")] = "HUGE"
        }
        assertEquals(
            WidgetStyle(backgroundPercent = 20, cornerRadiusDp = 32, textSize = WidgetTextSize.MEDIUM),
            DataStoreWidgetStyleRepository(store).observe().first(),
        )
    }

    @Test
    fun `the background can never be fully clear`() {
        assertEquals(20, WidgetStyle(backgroundPercent = 0).coerced().backgroundPercent)
        assertEquals(0.2f, WidgetStyle(backgroundPercent = 0).coerced().backgroundAlpha, 0.0001f)
    }

    @Test
    fun `text size scales are in order`() {
        assert(WidgetTextSize.SMALL.scale < WidgetTextSize.MEDIUM.scale)
        assert(WidgetTextSize.MEDIUM.scale < WidgetTextSize.LARGE.scale)
        assertEquals(1f, WidgetTextSize.MEDIUM.scale, 0f)
    }
}
