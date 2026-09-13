package com.learnhuayu.core.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnHuayuThemeTokensTest {

    @Test
    fun `light scheme uses the brand primary`() {
        assertThat(LearnHuayuLightColorScheme.primary).isEqualTo(Color(0xFF0FA3A3))
    }

    @Test
    fun `dark scheme keeps the brand primary and accent`() {
        assertThat(LearnHuayuDarkColorScheme.primary).isEqualTo(Color(0xFF0FA3A3))
        assertThat(LearnHuayuDarkColorScheme.secondary).isEqualTo(Color(0xFFFF6B4A))
    }

    @Test
    fun `spacing meets the accessibility touch target floor`() {
        assertThat(LearnHuayuSpacing().minimumTouchTarget.value).isAtLeast(48f)
    }

    @Test
    fun `drill tap target is large`() {
        assertThat(LearnHuayuSpacing().drillTapTargetMinHeight.value).isAtLeast(88f)
    }

    @Test
    fun `pinyin headline is larger than the body gloss`() {
        val headlineSize = LearnHuayuTypography.headlineSmall.fontSize
        val bodySize = LearnHuayuTypography.bodyMedium.fontSize

        assertThat(headlineSize.value).isGreaterThan(bodySize.value)
    }
}
