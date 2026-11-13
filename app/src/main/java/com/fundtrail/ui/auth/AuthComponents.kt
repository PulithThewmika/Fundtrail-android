package com.fundtrail.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.fundtrail.ui.common.Motion
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import com.fundtrail.ui.common.hero
import com.fundtrail.ui.common.HeroSurface
import com.fundtrail.ui.common.onHero
import com.fundtrail.ui.theme.Spacing
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.fundtrail.R
import com.fundtrail.ui.common.card
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val FieldShape = RoundedCornerShape(28.dp)

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes error: Int?,
    enabled: Boolean,
    isPassword: Boolean = false,
) {
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = true,
        enabled = enabled,
        shape = FieldShape,
        leadingIcon = { Icon(if (isPassword) Icons.Default.Lock else Icons.Default.Email, contentDescription = null) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(if (revealed) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                }
            }
        } else {
            null
        },
        // Light: a soft fill with no border reads against the white sheet. Dark: the sheet and a soft fill are almost
        // the same grey, so the field gets a lighter fill and a visible outline instead.
        colors = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedBorderColor = Color.Transparent,
            )
        } else {
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            )
        },
        visualTransformation = if (isPassword && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun FormError(@StringRes error: Int?) {
    // Remember the last message so the text stays readable while the block collapses.
    var last by remember { mutableStateOf(error) }
    if (error != null) last = error
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically(tween(Motion.FAST)) + fadeIn(tween(Motion.FAST)),
        exit = shrinkVertically(tween(Motion.FAST)) + fadeOut(tween(100)),
    ) {
        last?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** "or" between the password form and the Google button, so the two sign-in paths read as distinct options. */
@Composable
fun AuthDivider(label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

/**
 * Shared chrome for the pre-login screens and onboarding: a dark green page with the title centred at the
 * top, and the form on a white sheet with rounded top corners that runs to the bottom edge.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val hero = MaterialTheme.colorScheme.hero
    val onHero = MaterialTheme.colorScheme.onHero
    val sheet = MaterialTheme.colorScheme.card
    val ring = onHero.copy(alpha = 0.07f)
    // One header artwork for light and dark mode. Its top is bright green, so the status bar always uses dark icons here.
    // Switch to white icons if the artwork is replaced with a dark one.
    LightStatusBarIcons(whiteIcons = false)
    // The host keeps content below the status bar. This screen reaches up behind it so the artwork is not cut
    // by a flat band: it is measured taller by the inset and placed that far up.
    val topInset = WindowInsets.statusBars.getTop(LocalDensity.current)
    BoxWithConstraints(
        Modifier.fillMaxSize().layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight + topInset))
            layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, -topInset) }
        },
    ) {
    // The sheet is as tall as its form needs. The artwork takes what is left, but never less than a strip
    // (a short phone) and never more than about 37% (a tall one), so the form is not pushed off a small screen.
    val minSheet = maxHeight * 0.63f
    val maxSheet = maxHeight - 120.dp
    val overlap = 28.dp
    Column(
        Modifier
            .fillMaxSize()
            .drawBehind {
                // Paint the sheet colour below, so the gesture bar does not show a strip of the page colour.
                val bleed = 120.dp.toPx()
                drawRect(sheet, topLeft = Offset(0f, size.height), size = Size(size.width, bleed))
            }
            .background(hero),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                // The artwork is shown as it is, with no tint over it.
                .paint(painterResource(R.drawable.bg_auth_header), contentScale = ContentScale.Crop),
        )
        Column(
            Modifier
                // The sheet is drawn over the bottom of the artwork, so its rounded corners cut into the picture.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val shift = overlap.roundToPx()
                    layout(placeable.width, placeable.height - shift) { placeable.place(0, -shift) }
                }
                .fillMaxWidth()
                .heightIn(min = minSheet, max = maxSheet)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(sheet)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // On a tablet or in landscape the form stays a readable column instead of stretching edge to edge.
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                content()
            }
        }
    }
    }
}

// Screens that use light status bar icons overlap while navigating between them (the next one composes before the
// last one leaves), so the original setting is saved by the first user and restored only when the last one leaves.
private var lightIconUsers = 0
private var savedLightAppearance: Boolean? = null

/**
 * White status bar icons (or dark ones, with [whiteIcons] = false) while a screen with its own header artwork is showing;
 * the original setting returns when the last such screen leaves.
 */
@Composable
fun LightStatusBarIcons(whiteIcons: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            if (lightIconUsers++ == 0) savedLightAppearance = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = !whiteIcons
        }
        onDispose {
            if (controller != null && --lightIconUsers == 0) {
                savedLightAppearance?.let { controller.isAppearanceLightStatusBars = it }
            }
        }
    }
}
