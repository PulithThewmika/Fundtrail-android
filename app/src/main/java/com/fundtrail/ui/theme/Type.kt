package com.fundtrail.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.fundtrail.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** One variable font file (Inter, SIL Open Font License 1.1) serves every weight the scale uses. */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) =
    Font(R.font.inter, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** The one place the app's typeface is chosen. */
val AppFontFamily: FontFamily =
    FontFamily(inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold), inter(FontWeight.Bold))

/**
 * Tabular figures on every style: amounts line up in columns and don't shift width as digits change.
 * Tracking is tight on large sizes and near zero on body text; Material's default 0.5sp reads loose.
 */
private fun style(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = "tnum",
)

val Typography = Typography(
    // Display: hero amounts (safe to spend, balances).
    displayLarge = style(57, 64, FontWeight.Bold, -1.0),
    displayMedium = style(45, 52, FontWeight.Bold, -0.75),
    displaySmall = style(36, 44, FontWeight.Bold, -0.5),
    // Headline: screen titles.
    headlineLarge = style(32, 40, FontWeight.Bold, -0.5),
    headlineMedium = style(28, 36, FontWeight.Bold, -0.4),
    headlineSmall = style(24, 32, FontWeight.SemiBold, -0.25),
    // Title: card and section titles, list-row primary text.
    titleLarge = style(20, 28, FontWeight.SemiBold, -0.15),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal, 0.1),
    // Label: buttons, chips, overlines.
    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(12, 16, FontWeight.Medium, 0.2),
    labelSmall = style(11, 16, FontWeight.Medium, 0.4),
)
