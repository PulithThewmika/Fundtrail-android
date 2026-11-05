package com.fundtrail.ui.welcome

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fundtrail.R
import com.fundtrail.ui.theme.FundtrailAppTheme
import com.fundtrail.ui.theme.PrimaryContainerDark
import com.fundtrail.ui.theme.PrimaryContainerLight
import kotlinx.coroutines.launch

private enum class WelcomeIllustration { WALLET, SAFE_TO_SPEND, GOAL }

private data class WelcomePage(
    @androidx.annotation.StringRes val headline: Int,
    @androidx.annotation.StringRes val subtitle: Int,
    @androidx.annotation.StringRes val description: Int,
    val illustration: WelcomeIllustration,
)

private val pages = listOf(
    WelcomePage(R.string.welcome_headline_track, R.string.welcome_subtitle_track, R.string.welcome_illustration_track, WelcomeIllustration.WALLET),
    WelcomePage(R.string.welcome_headline_safe, R.string.welcome_subtitle_safe, R.string.welcome_illustration_safe, WelcomeIllustration.SAFE_TO_SPEND),
    WelcomePage(R.string.welcome_headline_goal, R.string.welcome_subtitle_goal, R.string.welcome_illustration_goal, WelcomeIllustration.GOAL),
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
    Column(Modifier.fillMaxSize().background(PrimaryContainerDark)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            WelcomeHero(pages[page].illustration, pages[page].description)
        }
        WelcomeSheet(
            page = pages[pagerState.currentPage],
            pageCount = pages.size,
            selectedPage = pagerState.currentPage,
            onNext = {
                val next = pagerState.currentPage + 1
                if (next < pages.size) scope.launch { pagerState.animateScrollToPage(next) } else onGetStarted()
            },
        )
    }
}

@Composable
private fun WelcomeHero(illustration: WelcomeIllustration, @androidx.annotation.StringRes description: Int) {
    val label = stringResource(description)
    Box(Modifier.fillMaxSize().semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            when (illustration) {
                WelcomeIllustration.WALLET -> drawWallet()
                WelcomeIllustration.SAFE_TO_SPEND -> drawSafeToSpendDial()
                WelcomeIllustration.GOAL -> drawGoalTarget()
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWallet() {
    val mint = PrimaryContainerLight
    val wallet = Color(0xFF1A6B56)
    val coin = Color(0xFF3FC496)

    val walletTop = size.height * 0.42f
    val walletHeight = size.height * 0.38f
    drawRoundRect(
        color = wallet,
        topLeft = Offset(size.width * 0.14f, walletTop),
        size = Size(size.width * 0.72f, walletHeight),
        cornerRadius = CornerRadius(20f, 20f),
    )
    drawRoundRect(
        color = mint,
        topLeft = Offset(size.width * 0.14f, walletTop),
        size = Size(size.width * 0.72f, walletHeight * 0.3f),
        cornerRadius = CornerRadius(20f, 20f),
    )
    drawCircle(color = PrimaryContainerDark, radius = size.width * 0.045f, center = Offset(size.width * 0.78f, walletTop + walletHeight * 0.5f))

    val coinCenterY = size.height * 0.28f
    drawOval(color = coin, topLeft = Offset(size.width * 0.3f, coinCenterY + 8f), size = Size(size.width * 0.4f, size.height * 0.1f))
    drawOval(color = mint, topLeft = Offset(size.width * 0.3f, coinCenterY - 8f), size = Size(size.width * 0.4f, size.height * 0.1f))

    val trend = Path().apply {
        moveTo(size.width * 0.08f, size.height * 0.56f)
        lineTo(size.width * 0.28f, size.height * 0.40f)
        lineTo(size.width * 0.44f, size.height * 0.48f)
        lineTo(size.width * 0.62f, size.height * 0.28f)
    }
    drawPath(trend, mint, style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(color = mint, radius = 10f, center = Offset(size.width * 0.62f, size.height * 0.28f))
}

/** A speedometer-style dial, three-quarters full - echoes the dashboard's own Safe to spend card. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSafeToSpendDial() {
    val mint = PrimaryContainerLight
    val track = Color(0xFF1A6B56)
    val needle = Color(0xFFF7F5EF)

    val diameter = size.minDimension * 0.72f
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f + size.height * 0.04f)
    val arcSize = Size(diameter, diameter)
    val stroke = Stroke(width = diameter * 0.12f, cap = StrokeCap.Round)

    drawArc(track, startAngle = 150f, sweepAngle = 240f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
    drawArc(mint, startAngle = 150f, sweepAngle = 165f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)

    val center = Offset(size.width / 2f, (size.height / 2f) + size.height * 0.04f)
    val needleAngleDeg = 150f + 165f
    val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
    val needleLength = diameter * 0.32f
    val tip = Offset(
        center.x + (kotlin.math.cos(needleAngleRad) * needleLength).toFloat(),
        center.y + (kotlin.math.sin(needleAngleRad) * needleLength).toFloat(),
    )
    drawLine(needle, center, tip, strokeWidth = 7f, cap = StrokeCap.Round)
    drawCircle(needle, radius = 9f, center = center)
}

/** A flag planted on a target ring, with a progress bar underneath - the dashboard's own goal card shape. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoalTarget() {
    val mint = PrimaryContainerLight
    val ring = Color(0xFF1A6B56)
    val flagPole = Color(0xFFF7F5EF)
    val flagCloth = Color(0xFF3FC496)

    val ringDiameter = size.minDimension * 0.5f
    val ringTopLeft = Offset((size.width - ringDiameter) / 2f, size.height * 0.1f)
    val innerDiameter = ringDiameter * 0.6f
    val innerTopLeft = Offset(ringTopLeft.x + (ringDiameter - innerDiameter) / 2f, ringTopLeft.y + (ringDiameter - innerDiameter) / 2f)
    drawArc(ring, 0f, 360f, useCenter = false, topLeft = ringTopLeft, size = Size(ringDiameter, ringDiameter), style = Stroke(width = ringDiameter * 0.1f))
    drawArc(mint, 0f, 360f, useCenter = false, topLeft = innerTopLeft, size = Size(innerDiameter, innerDiameter), style = Stroke(width = ringDiameter * 0.1f))

    val poleBase = Offset(size.width / 2f, ringTopLeft.y + ringDiameter / 2f)
    val poleTop = Offset(poleBase.x, poleBase.y - ringDiameter * 0.55f)
    drawLine(flagPole, poleBase, poleTop, strokeWidth = 6f, cap = StrokeCap.Round)
    val flag = Path().apply {
        moveTo(poleTop.x, poleTop.y)
        lineTo(poleTop.x + ringDiameter * 0.32f, poleTop.y + ringDiameter * 0.1f)
        lineTo(poleTop.x, poleTop.y + ringDiameter * 0.2f)
        close()
    }
    drawPath(flag, flagCloth)

    val barTop = size.height * 0.62f
    val barWidth = size.width * 0.6f
    val barLeft = (size.width - barWidth) / 2f
    drawRoundRect(ring, topLeft = Offset(barLeft, barTop), size = Size(barWidth, 14f), cornerRadius = CornerRadius(7f, 7f))
    drawRoundRect(mint, topLeft = Offset(barLeft, barTop), size = Size(barWidth * 0.55f, 14f), cornerRadius = CornerRadius(7f, 7f))
}

@Composable
private fun WelcomeSheet(page: WelcomePage, pageCount: Int, selectedPage: Int, onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        PageDots(pageCount = pageCount, selected = selectedPage, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(page.headline),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(page.subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(22.dp))
        val isLast = selectedPage == pageCount - 1
        Button(
            onClick = onNext,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text(stringResource(if (isLast) R.string.welcome_cta else R.string.welcome_next), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PageDots(pageCount: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        repeat(pageCount) { index ->
            if (index > 0) Spacer(Modifier.size(6.dp))
            val color = if (index == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
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
