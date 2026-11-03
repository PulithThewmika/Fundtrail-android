package com.fundtrail.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** The platform entry point the launcher talks to; declared in the manifest with `fundtrail_widget_info` (#226). */
class FundtrailWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FundtrailWidget()
}
