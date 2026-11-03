package com.fundtrail.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

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
        val chosen = WidgetStyle(backgroundPercent = 75, cornerRadiusDp = 28, textSize = WidgetTextSize.LARGE)
        repo.save(chosen)
        assertEquals(chosen, repo.observe().first())
    }

    @Test
    fun `saving a look outside the allowed range stores the nearest allowed one`() = runTest {
        val repo = DataStoreWidgetStyleRepository(dataStore())
        repo.save(WidgetStyle(backgroundPercent = 0, cornerRadiusDp = 500))
        assertEquals(
            WidgetStyle(backgroundPercent = WidgetStyle.MIN_BACKGROUND_PERCENT, cornerRadiusDp = 32),
            repo.observe().first(),
        )
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
            WidgetStyle(
                backgroundPercent = WidgetStyle.MIN_BACKGROUND_PERCENT,
                cornerRadiusDp = 32,
                textSize = WidgetTextSize.MEDIUM,
            ),
            DataStoreWidgetStyleRepository(store).observe().first(),
        )
    }

    @Test
    fun `the background never goes below the readable floor`() {
        val floor = WidgetStyle(backgroundPercent = 0).coerced()
        assertEquals(WidgetStyle.MIN_BACKGROUND_PERCENT, floor.backgroundPercent)
        assertEquals(WidgetStyle.MIN_BACKGROUND_PERCENT / 100f, floor.backgroundAlpha, 0.0001f)
    }

    /** Fails the first read, then behaves. A plain catch would end the stream on that first failure. */
    private class FlakyPreferences(private val saved: Preferences) : DataStore<Preferences> {
        var failuresLeft = 1
        override val data: Flow<Preferences> = flow {
            if (failuresLeft > 0) {
                failuresLeft--
                throw IOException("disk")
            }
            emit(saved)
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = saved
    }

    @Test
    fun `an unreadable store gives the default look and then recovers`() = runTest {
        val saved = mutablePreferencesOf(intPreferencesKey("widget_background_percent") to 80)
        val repo = DataStoreWidgetStyleRepository(FlakyPreferences(saved))

        val seen = repo.observe().take(2).toList()

        assertEquals(WidgetStyle.DEFAULT_BACKGROUND_PERCENT, seen[0].backgroundPercent)
        assertEquals(80, seen[1].backgroundPercent)
    }

    @Test
    fun `text size scales are in order`() {
        assert(WidgetTextSize.SMALL.scale < WidgetTextSize.MEDIUM.scale)
        assert(WidgetTextSize.MEDIUM.scale < WidgetTextSize.LARGE.scale)
        assertEquals(1f, WidgetTextSize.MEDIUM.scale, 0f)
    }
}
