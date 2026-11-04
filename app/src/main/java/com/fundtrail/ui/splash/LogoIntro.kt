package com.fundtrail.ui.splash

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fundtrail.R
import com.fundtrail.ui.common.systemAnimationsEnabled
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Matches the system splash background in `values-v31/themes.xml`, so the hand-over doesn't flash. */
val BrandBackground = Color(0xFFF5F4F0)
private val Mint = Color(0xFF8CF8C7)

/** Expo-out: fast start, long soft landing. Reads as "drawn" rather than "faded". */
private val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

// The three layers are cropped to one shared 723×552 box, so stacking them restores the logo exactly.
// The fractions below are positions inside that box, measured from the artwork.
private const val LOGO_ASPECT = 723f / 552f
private val TrailStart = Offset(-0.05f, 1.05f) // just past the dot, bottom-left
private val TrailEnd = Offset(0.56f, 0.29f)    // just past the arrow tip, top-right
private val ArrowTip = Offset(0.53f, 0.32f)
private val FundBox = floatArrayOf(0.03f, 0.02f, 0.99f, 0.50f)  // left, top, right, bottom
private val TrailBox = floatArrayOf(0.20f, 0.36f, 0.99f, 0.97f)

/**
 * Cold-start logo, about 1.6 s:
 * 1. the green trail is drawn on from the dot to the arrow with a soft leading edge, drifting up-right on a spring;
 * 2. a mint glint flares at the arrow tip as it lands;
 * 3. "Fund" then "Trail" rise out of their own baselines (masked, not faded);
 * 4. the mark eases forward and dissolves into the app.
 * Calls [onDone] when finished, or at once when the user has turned animations off.
 */
@Composable
fun LogoIntro(onDone: () -> Unit) {
    val animationsOff = !systemAnimationsEnabled()

    val draw = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val glint = remember { Animatable(0f) }
    val fund = remember { Animatable(0f) }
    val trail = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (animationsOff) {
            onDone()
            return@LaunchedEffect
        }
        coroutineScope {
            launch { draw.animateTo(1f, tween(750, easing = ExpoOut)) }
            launch { lift.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            launch { delay(450); glint.animateTo(1f, tween(650)) }
            launch { delay(280); fund.animateTo(1f, tween(600, easing = ExpoOut)) }
            launch { delay(400); trail.animateTo(1f, tween(600, easing = ExpoOut)) }
        }
        delay(300)
        exit.animateTo(1f, tween(280, easing = FastOutLinearInEasing))
        onDone()
    }

    Box(Modifier.fillMaxSize().background(BrandBackground), Alignment.Center) {
        val name = stringResource(R.string.app_name)
        Box(
            Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .aspectRatio(LOGO_ASPECT)
                .semantics { contentDescription = name }
                .graphicsLayer {
                    val s = 1f + 0.06f * exit.value
                    scaleX = s; scaleY = s
                    alpha = 1f - exit.value
                },
        ) {
            Layer(R.drawable.logo_fund, Modifier.maskedRise(fund::value, FundBox))
            Layer(R.drawable.logo_trail_word, Modifier.maskedRise(trail::value, TrailBox))
            Layer(
                R.drawable.logo_green,
                Modifier
                    .graphicsLayer {
                        translationX = -(1f - lift.value) * size.width * 0.04f
                        translationY = (1f - lift.value) * size.height * 0.06f
                        compositingStrategy = CompositingStrategy.Offscreen // needed for the DstIn mask
                    }
                    .drawWithContent {
                        val p = draw.value
                        if (p <= 0f) return@drawWithContent
                        drawContent()
                        // Opaque behind the head, transparent ahead of it, with a soft 12% edge: a pen, not a wipe.
                        val edge = 0.12f
                        val head = p * (1f + edge)
                        drawRect(
                            Brush.linearGradient(
                                0f to Color.Black,
                                (head - edge).coerceIn(0f, 1f) to Color.Black,
                                head.coerceIn(0f, 1f) to Color.Transparent,
                                1f to Color.Transparent,
                                start = Offset(TrailStart.x * size.width, TrailStart.y * size.height),
                                end = Offset(TrailEnd.x * size.width, TrailEnd.y * size.height),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
            // Glint at the arrow tip: grows and fades out as the arrow lands.
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    val g = glint.value
                    if (g <= 0f || g >= 1f) return@drawWithContent
                    val c = Offset(ArrowTip.x * size.width, ArrowTip.y * size.height)
                    val r = size.width * (0.04f + 0.14f * g)
                    drawCircle(
                        Brush.radialGradient(listOf(Mint.copy(alpha = 0.7f * (1f - g)), Color.Transparent), c, r),
                        radius = r,
                        center = c,
                    )
                },
            )
        }
    }
}

/** Slides the layer up out of its own box, clipped to that box, so the word appears to rise from its baseline. */
private fun Modifier.maskedRise(progress: () -> Float, box: FloatArray) = drawWithContent {
    val l = box[0] * size.width
    val t = box[1] * size.height
    val r = box[2] * size.width
    val b = box[3] * size.height
    clipRect(l, t, r, b) {
        translate(top = (1f - progress()) * (b - t)) { this@drawWithContent.drawContent() }
    }
}

@Composable
private fun Layer(@DrawableRes id: Int, modifier: Modifier) =
    Image(painterResource(id), contentDescription = null, modifier = Modifier.fillMaxSize().then(modifier))
