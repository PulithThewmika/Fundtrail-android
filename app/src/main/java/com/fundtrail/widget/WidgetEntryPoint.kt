package com.fundtrail.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * How the widget reaches the app's Hilt graph (#226). A `GlanceAppWidget` is not an Android entry point, so
 * `provideGlance` asks for this with `EntryPointAccessors.fromApplication`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetStateProvider(): WidgetStateProvider
}
