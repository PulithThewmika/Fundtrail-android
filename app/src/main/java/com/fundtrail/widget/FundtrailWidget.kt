package com.fundtrail.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
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
 * `provideGlance` loads a first snapshot through the Hilt entry point, and the composition then keeps watching
 * the live state and the chosen look. That matters: a Glance session can outlive one refresh, so a snapshot taken
 * once would keep showing an old message (for example "Sign in") after the data changed.
 * The composable only draws; the figures come from the domain use cases.
 */
class FundtrailWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val states = entryPoint.widgetStateProvider()
        val styles = entryPoint.widgetStyleRepository()
        val firstState = states.load()
        val firstStyle = styles.current()
        provideContent {
            val state by states.observe().collectAsState(firstState)
            val style by styles.observe().collectAsState(firstStyle)
            GlanceTheme { WidgetContent(context, state, style) }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, state: WidgetState, style: WidgetStyle) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    val base = GlanceModifier
        .fillMaxSize()
        .background(widgetBackground(style))
        .cornerRadius(style.cornerRadiusDp.dp)
        .padding(12.dp)
    when (state) {
        is WidgetState.Ready -> ReadyContent(context, state, style, base)
        WidgetState.SignedOut -> MessageContent(context.getString(R.string.widget_sign_in), style, base.clickable(openApp))
        WidgetState.Unavailable -> MessageContent(context.getString(R.string.widget_open_app), style, base.clickable(openApp))
    }
}

/**
 * Solid background keeps the phone's Material theme colour. A see-through one uses the standard light and dark
 * surface colours with the chosen opacity, so it still follows light and dark mode.
 */
@Composable
private fun widgetBackground(style: WidgetStyle) =
    if (style.backgroundPercent >= WidgetStyle.MAX_BACKGROUND_PERCENT) {
        GlanceTheme.colors.widgetBackground
    } else {
        ColorProvider(
            day = Color(LIGHT_SURFACE).copy(alpha = style.backgroundAlpha),
            night = Color(DARK_SURFACE).copy(alpha = style.backgroundAlpha),
        )
    }

@Composable
private fun ReadyContent(context: Context, state: WidgetState.Ready, style: WidgetStyle, modifier: GlanceModifier) {
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Label(context.getString(R.string.widget_today_label), style)
                Value(MoneyFormatter.formatLkr(state.todaySpendMinor), style)
            }
            // Opens the look settings; a plain tap target because a widget cannot have a menu.
            Text(
                text = SETTINGS_GLYPH,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (18 * style.textSize.scale).sp),
                modifier = GlanceModifier
                    .padding(start = 8.dp, bottom = 4.dp)
                    .clickable(actionStartActivity(WidgetSettingsActivity.intent(context))),
            )
        }
        state.safeToSpend?.let { safe ->
            safeToSpendText(context, safe)?.let { text ->
                Spacer(GlanceModifier.height(6.dp))
                Label(context.getString(R.string.widget_safe_label), style)
                Value(text, style)
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
private fun MessageContent(text: String, style: WidgetStyle, modifier: GlanceModifier) {
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = (15 * style.textSize.scale).sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun Label(text: String, style: WidgetStyle) {
    Text(text, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (12 * style.textSize.scale).sp))
}

@Composable
private fun Value(text: String, style: WidgetStyle) {
    Text(
        text,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = (18 * style.textSize.scale).sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/** The short wording per state; null hides the row (nothing to say before there is any income). */
private fun safeToSpendText(context: Context, safe: SafeToSpend): String? = when (safe.state) {
    SafeToSpendState.AVAILABLE -> MoneyFormatter.formatLkr(safe.amountMinor)
    SafeToSpendState.FULLY_USED -> context.getString(R.string.widget_safe_used)
    SafeToSpendState.WAITING_FOR_SALARY -> context.getString(R.string.widget_safe_waiting)
    SafeToSpendState.NOT_ENOUGH_DATA -> null
}

private const val SETTINGS_GLYPH = "⚙"
private const val LIGHT_SURFACE = 0xFFFFFBFEL
private const val DARK_SURFACE = 0xFF1C1B1FL
