package com.fundtrail.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.fundtrail.R
import com.fundtrail.ui.theme.Spacing
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** The app's shared building blocks. Every screen is made from these, so they all read as one product. */

/** Card surface: white on the tinted light page, one step lifted on the dark one. */
val ColorScheme.card: Color
    get() = if (surface.luminance() > 0.5f) surfaceContainerLowest else surfaceContainerLow

/** [HeroAmount] that counts to [amountMinor] (from zero the first time, from the old value after), then shows it exactly. */
@Composable
fun CountingHeroAmount(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    val animate = systemAnimationsEnabled()
    var shown by remember { mutableLongStateOf(if (animate) 0L else amountMinor) }
    LaunchedEffect(amountMinor, animate) {
        if (!animate) {
            shown = amountMinor
            return@LaunchedEffect
        }
        val from = shown
        animate(0f, 1f, animationSpec = tween(500, easing = Motion.Decelerate)) { fraction, _ ->
            shown = from + ((amountMinor - from) * fraction.toDouble()).toLong()
        }
        shown = amountMinor
    }
    HeroAmount(shown, modifier, color, style)
}

/** The lead surface of a screen (the dashboard figure): solid brand green, never a gradient. */
val ColorScheme.hero: Color
    get() = if (surface.luminance() > 0.5f) primary else primaryContainer

val ColorScheme.onHero: Color
    get() = if (surface.luminance() > 0.5f) onPrimary else onPrimaryContainer

/**
 * Frame for every screen: one header (large title on a tab, back arrow and smaller title on a child screen),
 * then the content. The host scaffold has already applied the system bar insets, so none are added here.
 */
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // How far the content has scrolled; the header shadow shows only once it has moved off the top.
    var scrolled by remember { mutableFloatStateOf(0f) }
    val tracker = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                scrolled = (scrolled - consumed.y).coerceAtLeast(0f)
                return Offset.Zero
            }
        }
    }
    val shadow by animateDpAsState(if (scrolled > 1f) 16.dp else 0.dp, label = "headerShadow")
    Scaffold(
        modifier = modifier.nestedScroll(tracker),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = shadow) {
                // The header and the content share one column width, so they line up on a wide screen.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = MaxContentWidth)) { ScreenHeader(title, onBack, actions) }
                }
            }
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
    ) { padding ->
        // On a tablet, a foldable or in landscape the content stays a readable column instead of stretching edge to edge.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = MaxContentWidth).fillMaxHeight()) { content(padding) }
        }
    }
}

/** The widest a screen's content gets; narrower windows (every phone) are unaffected. */
val MaxContentWidth = 640.dp

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(start = if (onBack != null) Spacing.xs else Spacing.screen, end = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.accessibility_back))
            }
        }
        Text(
            title,
            style = if (onBack != null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        actions()
    }
}

/** Previous / next month, on its own row so a large font never squeezes it against the title. */
@Composable
fun MonthSwitcher(
    month: YearMonth,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = Spacing.screen), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.dashboard_previous_month))
        }
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val dir = if (targetState.isAfter(initialState)) 1 else -1
                (fadeIn(tween(Motion.STANDARD)) + slideInHorizontally(tween(Motion.STANDARD, easing = Motion.Decelerate)) { it / 4 * dir }) togetherWith
                    (fadeOut(tween(100)) + slideOutHorizontally(tween(Motion.STANDARD, easing = Motion.Decelerate)) { -it / 4 * dir })
            },
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
            label = "month",
        ) { shownMonth ->
            Text(
                shownMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        FilledTonalIconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.dashboard_next_month))
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.card,
    contentPadding: Dp = Spacing.lg,
    spacing: Dp = Spacing.sm,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale(source) else Modifier)
            .animateContentSize(tween(Motion.STANDARD))
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Quiet label at the top of a card or above a group of rows. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(modifier.size(size).background(container, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}

/** The one list row: icon, title and optional subtitle, then an amount, switch or chevron. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    },
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        icon?.let { IconBadge(it) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

/** Green for a figure in a good range, red for one that needs attention. For numbers and status chips only. */
val ColorScheme.positive: Color
    get() = primary

val ColorScheme.negative: Color
    get() = error

/**
 * The lead card of a screen: solid brand green with two faint circles in the corner for depth (no gradient).
 * Content colour is [onHero].
 */
@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    spacing: Dp = Spacing.xs,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ring = MaterialTheme.colorScheme.onHero.copy(alpha = 0.07f)
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.hero)
            .drawBehind {
                val corner = Offset(size.width, 0f)
                drawCircle(ring, radius = size.width * 0.55f, center = corner)
                drawCircle(ring, radius = size.width * 0.32f, center = corner)
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** A translucent pill on a [HeroSurface]. */
@Composable
fun HeroPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val onHero = MaterialTheme.colorScheme.onHero
    Row(
        modifier.background(onHero.copy(alpha = 0.14f), CircleShape).padding(horizontal = Spacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = onHero, modifier = Modifier.size(16.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = onHero)
    }
}

/** A figure in its own small card: icon badge, label, value. Sits two to a row. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    AppCard(modifier, spacing = Spacing.xs) {
        IconBadge(icon, size = 36.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

/** A labelled figure on a recessed tile, for grids of secondary numbers inside a card. */
@Composable
fun InsetStat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

/** Progress as a ring with the figure in the middle. [fraction] is clamped to 0..1. */
@Composable
fun ProgressRing(
    fraction: Float,
    centerText: String,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 9.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    textColor: Color = Color.Unspecified,
) {
    val animate = systemAnimationsEnabled()
    val filled = remember { Animatable(if (animate) 0f else fraction.coerceIn(0f, 1f)) }
    LaunchedEffect(fraction, animate) {
        if (animate) filled.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = Motion.Decelerate)) else filled.snapTo(fraction.coerceIn(0f, 1f))
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        CircularProgressIndicator(
            progress = { filled.value },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = trackColor,
        )
        Text(
            centerText,
            style = if (size >= 88.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

/**
 * A form in a bottom sheet: title, scrolling fields, then the full-width main action pinned under them.
 * Yes/no confirmations stay as dialogs; anything with fields uses this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    dismissLabel: String? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.lg),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = Spacing.lg).semantics { heading() },
            )
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = Spacing.lg)) {
                content()
            }
            PrimaryButton(confirmLabel, onClick = onConfirm, enabled = confirmEnabled, loading = loading)
            dismissLabel?.let {
                TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) { Text(it) }
            }
        }
    }
}

/** Full-width main action. While [loading] it keeps its size and shows a spinner instead of the label. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).pressScale(source),
    ) {
        ButtonContent(text, loading, icon)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    FilledTonalButton(onClick = onClick, enabled = enabled && !loading, modifier = modifier.heightIn(min = 52.dp)) {
        ButtonContent(text, loading, icon)
    }
}

@Composable
private fun ButtonContent(text: String, loading: Boolean, icon: ImageVector?) {
    // The button keeps its size (the label's row is as tall as the spinner), so only the contents swap.
    Crossfade(targetState = loading, animationSpec = tween(Motion.FAST), label = "buttonContent") { busy ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
            } else {
                icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = Spacing.sm).size(18.dp)) }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.background(container, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Rounded bar that eases to its value. [fraction] is clamped, so an over-target goal reads as full. */
@Composable
fun ProgressBar(
    fraction: Float,
    description: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 10.dp,
) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxWidth(shown).fillMaxHeight().background(color, CircleShape))
    }
}

/**
 * An amount for headline use: the currency code and the cents are set smaller than the whole number, so the
 * figure that matters is the one that is read first. Same text as [MoneyFormatter.formatLkr] for TalkBack.
 */
fun heroAmount(amountMinor: Long): AnnotatedString {
    val full = MoneyFormatter.formatLkr(amountMinor)
    val numberStart = full.indexOf(' ') + 1
    val decimalStart = full.lastIndexOf('.')
    val small = SpanStyle(fontSize = 0.5.em)
    return buildAnnotatedString {
        withStyle(small) { append(full.substring(0, numberStart)) }
        append(full.substring(numberStart, decimalStart))
        withStyle(small) { append(full.substring(decimalStart)) }
    }
}

@Composable
fun HeroAmount(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    Text(heroAmount(amountMinor), style = style, color = color, modifier = modifier)
}

/** The app's one error state: what went wrong, and a way to try again when there is one. */
@Composable
fun ErrorView(@StringRes message: Int, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    EmptyView(
        icon = Icons.Outlined.CloudOff,
        title = stringResource(message),
        modifier = modifier,
        actionLabel = onRetry?.let { stringResource(R.string.retry) },
        actionIcon = Icons.Outlined.Refresh,
        onAction = onRetry,
    )
}

/** Renders a [UiState] the same way everywhere: skeleton, empty state, error with retry, or the content. */
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onRetry: (() -> Unit)?,
    loading: @Composable () -> Unit,
    empty: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
) {
    // The kind of state is the key, so new data inside Success updates in place instead of replaying the fade.
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn(tween(Motion.STANDARD, delayMillis = 60)) togetherWith fadeOut(tween(100))
        },
        contentKey = { it::class },
        label = "uiState",
    ) { current ->
        // Each state's content is a stack of cards; the Column keeps the spacing the callers' own columns use.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            when (current) {
                UiState.Loading -> loading()
                UiState.Empty -> empty()
                is UiState.Error -> ErrorView(current.message, onRetry)
                is UiState.Success -> content(current.data)
            }
        }
    }
}
