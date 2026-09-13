package com.learnhuayu.core.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val Ink900 = Color(0xFF0B1220)
internal val Ink800 = Color(0xFF131C2E)
internal val Ink700 = Color(0xFF1D2942)
internal val InkOnDark = Color(0xFFECEEF2)
internal val MutedOnDark = Color(0xFFB6BECC)
internal val Paper = Color(0xFFF7F5F0)
internal val PaperDim = Color(0xFFDEDBD3)
internal val PaperContainer = Color(0xFFEDEAE4)
internal val PaperContainerHigh = Color(0xFFE8E5DE)
internal val PaperContainerHighest = Color(0xFFE2DFD8)
internal val BrandTeal = Color(0xFF0FA3A3)
internal val OnBrandTeal = Color(0xFF04211F)
internal val TealContainer = Color(0xFFC4EFEC)
internal val TealContainerDark = Color(0xFF0B4A4A)
internal val BrandCoral = Color(0xFFFF6B4A)
internal val OnBrandCoral = Color(0xFF3A1108)
internal val CoralContainer = Color(0xFFFFDCD2)
internal val CoralContainerDark = Color(0xFF6B2417)
internal val SuccessGreen = Color(0xFF2E9E6B)
internal val OnSuccessGreen = Color(0xFF062D1D)
internal val SuccessContainer = Color(0xFFC8F0DA)
internal val SuccessContainerDark = Color(0xFF17523A)
internal val ErrorRed = Color(0xFFD64545)
internal val ErrorContainer = Color(0xFFFFDAD6)
internal val ErrorContainerDark = Color(0xFF7A2420)
internal val MutedGrey = Color(0xFF6B7280)
internal val SurfaceVariantLight = Color(0xFFE7E4DD)
internal val OutlineLight = Color(0xFFC9CDD4)
internal val OnSurfaceVariantLight = Color(0xFF4A5162)

/**
 * Status colors that sit outside the Material color roles, from
 * `android/.../assets/branding/palette.json`.
 */
object LearnHuayuStatusColors {
    val Success: Color = SuccessGreen
    val Caution: Color = Color(0xFFE0A400)
    val Muted: Color = MutedGrey
}

val LearnHuayuLightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = OnBrandTeal,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnBrandTeal,
    secondary = BrandCoral,
    onSecondary = OnBrandCoral,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = OnBrandCoral,
    tertiary = SuccessGreen,
    onTertiary = OnSuccessGreen,
    tertiaryContainer = SuccessContainer,
    onTertiaryContainer = OnSuccessGreen,
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF3A0A0A),
    background = Paper,
    onBackground = Ink900,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink900,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = MutedGrey,
    outlineVariant = OutlineLight,
    scrim = Ink900,
    inverseSurface = Ink800,
    inverseOnSurface = InkOnDark,
    inversePrimary = Color(0xFF4FD1CD),
    surfaceDim = PaperDim,
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F1EB),
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperContainerHigh,
    surfaceContainerHighest = PaperContainerHighest,
)

val LearnHuayuDarkColorScheme = darkColorScheme(
    primary = BrandTeal,
    onPrimary = OnBrandTeal,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealContainer,
    secondary = BrandCoral,
    onSecondary = OnBrandCoral,
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = CoralContainer,
    tertiary = Color(0xFF4CC38A),
    onTertiary = OnSuccessGreen,
    tertiaryContainer = SuccessContainerDark,
    onTertiaryContainer = SuccessContainer,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0A0A),
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainer,
    background = Ink900,
    onBackground = InkOnDark,
    surface = Ink800,
    onSurface = InkOnDark,
    surfaceVariant = Ink700,
    onSurfaceVariant = MutedOnDark,
    outline = MutedGrey,
    outlineVariant = Color(0xFF2A3752),
    scrim = Color(0xFF000000),
    inverseSurface = InkOnDark,
    inverseOnSurface = Ink800,
    inversePrimary = BrandTeal,
    surfaceDim = Ink900,
    surfaceBright = Color(0xFF2D3852),
    surfaceContainerLowest = Color(0xFF070D18),
    surfaceContainerLow = Ink800,
    surfaceContainer = Color(0xFF172138),
    surfaceContainerHigh = Ink700,
    surfaceContainerHighest = Color(0xFF243152),
)
