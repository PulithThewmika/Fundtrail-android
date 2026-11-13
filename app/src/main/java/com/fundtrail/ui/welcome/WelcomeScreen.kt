package com.fundtrail.ui.welcome

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.fundtrail.ui.common.Motion
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.fundtrail.ui.auth.LightStatusBarIcons
import androidx.compose.ui.layout.ContentScale
import com.fundtrail.ui.common.hero
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import com.fundtrail.ui.common.HeroSurface
import com.fundtrail.ui.theme.Spacing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fundtrail.R
import com.fundtrail.ui.common.card
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.painterResource
import com.fundtrail.ui.common.PrimaryButton
import com.fundtrail.ui.theme.FundtrailAppTheme
import kotlinx.coroutines.launch


private data class WelcomePage(
    @androidx.annotation.StringRes val headline: Int,
    @androidx.annotation.StringRes val subtitle: Int,
    @androidx.annotation.StringRes val description: Int,
    @DrawableRes val illustration: Int,
)

private val pages = listOf(
    WelcomePage(R.string.welcome_headline_track, R.string.welcome_subtitle_track, R.string.welcome_illustration_track, R.drawable.bg_welcome_track),
    WelcomePage(R.string.welcome_headline_safe, R.string.welcome_subtitle_safe, R.string.welcome_illustration_safe, R.drawable.bg_welcome_safe),
    WelcomePage(R.string.welcome_headline_goal, R.string.welcome_subtitle_goal, R.string.welcome_illustration_goal, R.drawable.bg_welcome_goal),
)

/**
 * First screens a signed-out user sees after the logo intro (#UI-ENHANCE): a swipeable, three-page pitch
 * ending in a single call to action. Dots and the button live outside the pager so they read as fixed chrome
 * while the hero and copy swipe underneath.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val page = pages[pagerState.currentPage]
    val isLast = pagerState.currentPage == pages.size - 1
    val hero = MaterialTheme.colorScheme.hero
    LightStatusBarIcons()
    Box(Modifier.fillMaxSize().background(hero)) {
        // The artwork fills the whole screen, status bar included; it is a full scene, so it is cropped to fit.
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            Image(
                painterResource(pages[index].illustration),
                contentDescription = stringResource(pages[index].description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // A soft green fade behind the status bar keeps its white icons readable on the bright parts of the art.
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(hero.copy(alpha = 0.7f), Color.Transparent))),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.card)
                // The sheet colour continues under the gesture bar; the button stays above it.
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // On a tablet or in landscape the text and button stay a readable column instead of stretching edge to edge.
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                PageDots(pageCount = pages.size, selected = pagerState.currentPage, modifier = Modifier.fillMaxWidth())
                // A fixed minimum keeps the button still when a headline wraps to a different line count.
                Crossfade(
                    targetState = page,
                    animationSpec = tween(Motion.STANDARD),
                    modifier = Modifier.padding(top = Spacing.lg).heightIn(min = 148.dp),
                    label = "welcomeText",
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(stringResource(current.headline), style = MaterialTheme.typography.headlineLarge)
                        Text(
                            stringResource(current.subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                PrimaryButton(
                    stringResource(if (isLast) R.string.welcome_cta else R.string.welcome_next),
                    onClick = {
                        val next = pagerState.currentPage + 1
                        if (next < pages.size) scope.launch { pagerState.animateScrollToPage(next) } else onGetStarted()
                    },
                    modifier = Modifier.padding(top = Spacing.lg),
                )
            }
        }
    }
}

/** The current page is a wider pill in brand green; the others are small dots. */
@Composable
private fun PageDots(pageCount: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(pageCount) { index ->
            val width by animateDpAsState(if (index == selected) 28.dp else 8.dp, label = "pageDot")
            val color = if (index == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(width = width, height = 8.dp).clip(CircleShape).background(color))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    FundtrailAppTheme {
        WelcomeScreen(onGetStarted = {})
    }
}
