package com.learnhuayu.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing tokens for the shared UI. The drill target is deliberately large and
 * the touch target floor follows the accessibility minimum (NFR-7).
 */
@Immutable
data class LearnHuayuSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val screenHorizontal: Dp = 16.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val drillTapTargetMinHeight: Dp = 96.dp,
)

val LocalLearnHuayuSpacing = staticCompositionLocalOf { LearnHuayuSpacing() }

val MaterialTheme.spacing: LearnHuayuSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalLearnHuayuSpacing.current
