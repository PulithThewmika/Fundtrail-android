package com.fundtrail.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.theme.FundtrailAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Lets the user choose how the home-screen widget looks: background opacity, corner roundness and text size.
 *
 * The launcher opens it when a widget is added (and on "reconfigure" in newer Android), and the widget's own
 * gear opens it later. The widget is always added, even if the user just backs out, so the screen never blocks it.
 * The widget's size itself is changed on the home screen by dragging its edges, which the launcher handles.
 */
@AndroidEntryPoint
class WidgetSettingsActivity : ComponentActivity() {
    private val viewModel: WidgetSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // When opened as the widget's configure screen, say "OK" up front so any way of leaving still adds it.
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }
        setContent {
            FundtrailAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val style by viewModel.style.collectAsStateWithLifecycle()
                    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
                    WidgetSettingsScreen(
                        style = style,
                        enabled = loaded,
                        onOpacityChange = viewModel::onOpacityChange,
                        onCornerChange = viewModel::onCornerChange,
                        onTextSizeChange = viewModel::onTextSizeChange,
                        onReset = viewModel::onReset,
                        onSave = { viewModel.save { finish() } },
                    )
                }
            }
        }
    }

    companion object {
        /** A plain launch from the widget's gear (no widget id). */
        fun intent(context: Context): Intent =
            Intent(context, WidgetSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun WidgetSettingsScreen(
    style: WidgetStyle,
    enabled: Boolean,
    onOpacityChange: (Int) -> Unit,
    onCornerChange: (Int) -> Unit,
    onTextSizeChange: (WidgetTextSize) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.widget_settings_title), style = MaterialTheme.typography.headlineSmall)
        WidgetPreview(style)
        Text(
            stringResource(R.string.widget_settings_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SliderRow(
            label = stringResource(R.string.widget_settings_opacity),
            valueText = stringResource(R.string.widget_settings_percent, style.backgroundPercent),
            value = style.backgroundPercent.toFloat(),
            range = WidgetStyle.MIN_BACKGROUND_PERCENT.toFloat()..WidgetStyle.MAX_BACKGROUND_PERCENT.toFloat(),
            enabled = enabled,
            onChange = { onOpacityChange(it.toInt()) },
        )
        SliderRow(
            label = stringResource(R.string.widget_settings_corners),
            valueText = stringResource(R.string.widget_settings_dp, style.cornerRadiusDp),
            value = style.cornerRadiusDp.toFloat(),
            range = 0f..WidgetStyle.MAX_CORNER_RADIUS_DP.toFloat(),
            enabled = enabled,
            onChange = { onCornerChange(it.toInt()) },
        )

        Text(stringResource(R.string.widget_settings_text_size), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetTextSize.entries.forEach { size ->
                FilterChip(
                    selected = style.textSize == size,
                    onClick = { onTextSizeChange(size) },
                    enabled = enabled,
                    label = { Text(stringResource(size.label)) },
                )
            }
        }

        Text(
            stringResource(R.string.widget_settings_size_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReset, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.widget_settings_reset))
            }
            Button(onClick = onSave, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.widget_settings_save))
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
    }
}

/**
 * A copy of the widget drawn over a colourful backdrop, so the effect of opacity and roundness is easy to see.
 * It is only a preview: the real widget is drawn by Glance in [FundtrailWidget].
 */
@Composable
private fun WidgetPreview(style: WidgetStyle) {
    val surface = if (isSystemInDarkTheme()) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val onSurface = if (isSystemInDarkTheme()) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val scale = style.textSize.scale
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFFA726))))
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(style.cornerRadiusDp.dp))
                .background(surface.copy(alpha = style.backgroundAlpha))
                .padding(12.dp),
        ) {
            Text(stringResource(R.string.widget_today_label), color = onSurface.copy(alpha = 0.7f), fontSize = (12 * scale).sp)
            Text(MoneyFormatter.formatLkr(PREVIEW_TODAY_MINOR), color = onSurface, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.widget_safe_label), color = onSurface.copy(alpha = 0.7f), fontSize = (12 * scale).sp)
            Text(MoneyFormatter.formatLkr(PREVIEW_SAFE_MINOR), color = onSurface, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.widget_add), color = MaterialTheme.colorScheme.onPrimary, fontSize = (14 * scale).sp)
            }
        }
    }
}

private val WidgetTextSize.label: Int
    get() = when (this) {
        WidgetTextSize.SMALL -> R.string.widget_settings_text_small
        WidgetTextSize.MEDIUM -> R.string.widget_settings_text_medium
        WidgetTextSize.LARGE -> R.string.widget_settings_text_large
    }

private const val PREVIEW_TODAY_MINOR = 125_000L
private const val PREVIEW_SAFE_MINOR = 1_840_000L
