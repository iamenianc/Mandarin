package com.learnhuayu.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders pinyin with tone numbers, never diacritics or hanzi (ADR 0012, FR-17).
 * Emphasis is per syllable, by index into the rendered phrase, so feedback can point at
 * the syllable that needs work (ADR 0011).
 */
@Composable
fun PinyinText(
    pinyin: String,
    modifier: Modifier = Modifier,
    emphasizedSyllables: Set<Int> = emptySet(),
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    emphasizedStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
    ),
    color: Color = Color.Unspecified,
) {
    val syllables = remember(pinyin) { PinyinFormatting.syllables(pinyin) }
    val displayText = remember(syllables, emphasizedSyllables, emphasizedStyle) {
        buildAnnotatedString {
            syllables.forEachIndexed { index, syllable ->
                if (index > 0) append(" ")
                withStyle(if (index in emphasizedSyllables) emphasizedStyle else SpanStyle()) {
                    append(syllable.displayText)
                }
            }
        }
    }
    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Preview(name = "Pinyin with emphasis", showBackground = true)
@Composable
private fun PinyinTextPreview() {
    LearnHuayuTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            PinyinText(pinyin = "ni3 hao3", emphasizedSyllables = setOf(1))
            PinyinText(
                pinyin = "dui4 bu5 qi3",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
