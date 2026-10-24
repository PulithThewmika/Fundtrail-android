package com.fundtrail.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Where the widget's chosen look is kept (per device, like the reminder settings). */
interface WidgetStyleRepository {
    /** The saved look. Never fails: an unreadable store gives the default look. */
    fun observe(): Flow<WidgetStyle>

    suspend fun save(style: WidgetStyle)
}

/** The current look, or the default if it cannot be read. */
suspend fun WidgetStyleRepository.current(): WidgetStyle = observe().first()

@Singleton
class DataStoreWidgetStyleRepository @Inject constructor(
    private val preferences: DataStore<Preferences>,
) : WidgetStyleRepository {

    override fun observe(): Flow<WidgetStyle> = preferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            WidgetStyle(
                backgroundPercent = prefs[BACKGROUND_PERCENT] ?: WidgetStyle.DEFAULT_BACKGROUND_PERCENT,
                cornerRadiusDp = prefs[CORNER_RADIUS_DP] ?: WidgetStyle.DEFAULT_CORNER_RADIUS_DP,
                textSize = prefs[TEXT_SIZE]?.let { name -> WidgetTextSize.entries.firstOrNull { it.name == name } }
                    ?: WidgetTextSize.MEDIUM,
            ).coerced()
        }
        .distinctUntilChanged()

    override suspend fun save(style: WidgetStyle) {
        val safe = style.coerced()
        preferences.edit {
            it[BACKGROUND_PERCENT] = safe.backgroundPercent
            it[CORNER_RADIUS_DP] = safe.cornerRadiusDp
            it[TEXT_SIZE] = safe.textSize.name
        }
    }

    private companion object {
        val BACKGROUND_PERCENT = intPreferencesKey("widget_background_percent")
        val CORNER_RADIUS_DP = intPreferencesKey("widget_corner_radius_dp")
        val TEXT_SIZE = stringPreferencesKey("widget_text_size")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetStyleModule {
    @Binds
    abstract fun bindWidgetStyleRepository(impl: DataStoreWidgetStyleRepository): WidgetStyleRepository
}
