package com.fundtrail.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * The app's one empty (and soft-error) state: an icon on a tonal disc, a short title, an optional line of body text
 * and an optional action. Neutral wording and colours only: an empty month or list is information, not a warning
 * (A-03, A-22). Eases in once when it first appears; skipped when system animations are off.
 *
 * [illustration], when given, replaces the icon in the full-size form; the compact form always uses the icon.
 * [compact] is the inline form for inside cards, sheets and list sections: icon left, text right, smaller type.
 * [framed] puts the compact form on its own soft panel, for use where there is no surrounding card.
 */
@Composable
fun EmptyView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
    framed: Boolean = false,
    @DrawableRes illustration: Int? = null,
) {
    val animate = systemAnimationsEnabled()
    val enter = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        enter.animateTo(1f, tween(420, easing = EaseOut))
    }
    val entrance = Modifier.graphicsLayer {
        alpha = enter.value
        translationY = (1f - enter.value) * 12.dp.toPx()
    }

    if (compact) {
        Row(
            modifier
                .fillMaxWidth()
                .then(
                    if (framed) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(16.dp)
                    } else {
                        Modifier.padding(vertical = 4.dp)
                    },
                )
                .then(entrance),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconHalo(icon, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                body?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                        modifier = Modifier.padding(top = 2.dp),
                    ) { ActionContent(actionLabel, actionIcon) }
                }
            }
        }
        return
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .then(entrance),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration != null) {
            Image(painterResource(illustration), contentDescription = null, modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 320.dp).aspectRatio(1f))
        } else {
            IconHalo(icon, size = 72.dp)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 20.dp)
                .widthIn(max = 320.dp)
                .semantics { heading() },
        )
        body?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp).widthIn(max = 300.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(
                onClick = onAction,
                contentPadding = if (actionIcon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
                modifier = Modifier.padding(top = 20.dp),
            ) { ActionContent(actionLabel, actionIcon) }
        }
    }
}

@Composable
private fun ActionContent(label: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Box(Modifier.size(ButtonDefaults.IconSpacing))
    }
    Text(label)
}

/** The icon on a plain tonal disc. [size] is the disc's diameter. */
@Composable
private fun IconHalo(icon: ImageVector, size: Dp) {
    Box(Modifier.size(size).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(size * 0.45f))
    }
}
