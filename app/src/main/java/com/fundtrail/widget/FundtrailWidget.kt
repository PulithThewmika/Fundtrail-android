package com.fundtrail.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fundtrail.MainActivity
import com.fundtrail.R
import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.model.SafeToSpendState
import com.fundtrail.ui.common.MoneyFormatter
import dagger.hilt.android.EntryPointAccessors

/**
 * The quick-add home-screen widget (#226): today's spend, Safe to spend, and a "+ Add" button.
 *
 * `provideGlance` reads one [WidgetState] through the Hilt entry point and the composable only draws it.
 * Nothing here works anything out; the figures come from the domain use cases.
 */
class FundtrailWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val state = entryPoint.widgetStateProvider().load()
        provideContent {
            GlanceTheme { WidgetContent(context, state) }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, state: WidgetState) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    val base = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(16.dp)
        .padding(12.dp)
    when (state) {
        is WidgetState.Ready -> ReadyContent(context, state, base)
        WidgetState.SignedOut -> MessageContent(context.getString(R.string.widget_sign_in), base.clickable(openApp))
        WidgetState.Unavailable -> MessageContent(context.getString(R.string.widget_open_app), base.clickable(openApp))
    }
}

@Composable
private fun ReadyContent(context: Context, state: WidgetState.Ready, modifier: GlanceModifier) {
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Label(context.getString(R.string.widget_today_label))
        Value(MoneyFormatter.formatLkr(state.todaySpendMinor))
        state.safeToSpend?.let { safe ->
            safeToSpendText(context, safe)?.let { text ->
                Spacer(GlanceModifier.height(6.dp))
                Label(context.getString(R.string.widget_safe_label))
                Value(text)
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Button(
            text = context.getString(R.string.widget_add),
            onClick = actionStartActivity(QuickAddLink.intent(context)),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MessageContent(text: String, modifier: GlanceModifier) {
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
}

@Composable
private fun Value(text: String) {
    Text(text, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold))
}

/** The short wording per state; null hides the row (nothing to say before there is any income). */
private fun safeToSpendText(context: Context, safe: SafeToSpend): String? = when (safe.state) {
    SafeToSpendState.AVAILABLE -> MoneyFormatter.formatLkr(safe.amountMinor)
    SafeToSpendState.FULLY_USED -> context.getString(R.string.widget_safe_used)
    SafeToSpendState.WAITING_FOR_SALARY -> context.getString(R.string.widget_safe_waiting)
    SafeToSpendState.NOT_ENOUGH_DATA -> null
}
