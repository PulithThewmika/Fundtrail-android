package com.fundtrail.ui.common

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Loads from Room usually finish well inside this; a skeleton that shows for one frame reads as a glitch. */
private const val REVEAL_DELAY_MS = 150L
private const val SWEEP_MS = 1400

/** Shared sweep progress, so every block in one skeleton shimmers as a single light passing over the page. */
private val LocalShimmer = staticCompositionLocalOf<State<Float>?> { null }

/** False when the user has turned system animations off (Developer options or accessibility). */
@Composable
fun systemAnimationsEnabled(): Boolean = Settings.Global.getFloat(
    LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
) != 0f

/**
 * Placeholder for a screen's content while it loads: blocks shaped like the real layout, with one soft diagonal
 * light sweeping across all of them. Fades in after [REVEAL_DELAY_MS] (it still takes its space at once, so the
 * page doesn't jump), and TalkBack reads [description] once instead of every block.
 */
@Composable
fun Skeleton(
    description: String,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(REVEAL_DELAY_MS)
        revealed = true
    }
    val alpha by animateFloatAsState(if (revealed) 1f else 0f, tween(260), label = "skeletonReveal")
    val sweep = if (systemAnimationsEnabled()) {
        rememberInfiniteTransition(label = "skeletonSweep").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SWEEP_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "skeletonSweepProgress",
        )
    } else {
        null
    }
    CompositionLocalProvider(LocalShimmer provides sweep) {
        Column(
            modifier
                .clearAndSetSemantics { contentDescription = description }
                .graphicsLayer { this.alpha = alpha },
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** One placeholder block. Use inside [Skeleton] so it picks up the shared sweep. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    // A light band on light themes, a faint lift on dark ones; never a darker band, which reads as a shadow.
    val highlight = if (base.luminance() > 0.5f) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.07f)
    val sweep = LocalShimmer.current
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    var originX by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .onGloballyPositioned { originX = it.positionInRoot().x }
            .clip(shape)
            .background(base)
            .drawBehind {
                val p = sweep?.value ?: return@drawBehind
                val band = screenWidth * 0.45f
                // Centre of the light in screen space, travelling from off the left edge to off the right.
                val centre = -band + p * (screenWidth + 2 * band) - originX
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to highlight,
                        1f to Color.Transparent,
                        start = Offset(centre - band / 2, 0f),
                        end = Offset(centre + band / 2, band * 0.25f), // slight tilt
                    ),
                )
            },
    )
}

/** A rounded text-line placeholder; [fraction] of the available width. */
@Composable
fun SkeletonLine(fraction: Float, height: Dp = 12.dp, modifier: Modifier = Modifier) =
    SkeletonBlock(modifier.fillMaxWidth(fraction).height(height), CircleShape)

/** A fixed-width text-line placeholder, for amounts at the end of a row. */
@Composable
fun SkeletonPill(width: Dp, height: Dp = 12.dp, modifier: Modifier = Modifier) =
    SkeletonBlock(modifier.width(width).height(height), CircleShape)

@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) = SkeletonBlock(modifier.size(size), CircleShape)

/** Card-shaped frame matching the app's cards, holding skeleton lines. */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Label on the left, value on the right, like the app's amount rows. */
@Composable
fun SkeletonLabelValue(labelFraction: Float = 0.3f, valueWidth: Dp = 88.dp, height: Dp = 14.dp) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SkeletonLine(labelFraction, height)
        Gap()
        SkeletonPill(valueWidth, height)
    }
}

@Composable
private fun RowScope.Gap() = Box(Modifier.weight(1f))
