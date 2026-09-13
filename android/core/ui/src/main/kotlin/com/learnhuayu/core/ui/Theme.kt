package com.learnhuayu.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Theme entry point for every LearnHuayu screen: brand color schemes, typography, and
 * spacing tokens on top of Material 3.
 */
@Composable
fun LearnHuayuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme = if (darkTheme) LearnHuayuDarkColorScheme else LearnHuayuLightColorScheme,
    typography: Typography = LearnHuayuTypography,
    spacing: LearnHuayuSpacing = LearnHuayuSpacing(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLearnHuayuSpacing provides spacing) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

@Preview(name = "Theme light", showBackground = true)
@Composable
private fun LearnHuayuThemeLightPreview() {
    LearnHuayuTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.spacing.medium),
        ) {
            Text(
                text = "ni3 hao3",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "hello",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Theme dark", showBackground = true)
@Composable
private fun LearnHuayuThemeDarkPreview() {
    LearnHuayuTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.spacing.medium),
        ) {
            Text(
                text = "ni3 hao3",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "hello",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
