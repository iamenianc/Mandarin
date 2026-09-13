package com.learnhuayu.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

/**
 * Optional Hangul aid line. Rendered only when a Hangul string is supplied; callers pass
 * null while the aid is disabled (ADR 0004). Hangul is never tone-marked, so the tone
 * stays in the pinyin line above it.
 */
@Composable
fun HangulAidText(
    hangul: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val text = hangul?.trim()
    if (text.isNullOrEmpty()) return
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Preview(name = "Pinyin with Hangul aid", showBackground = true)
@Composable
private fun HangulAidTextPreview() {
    LearnHuayuTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            PinyinText(pinyin = "ni3 hao3")
            HangulAidText(hangul = "니 하오")
            PinyinText(pinyin = "xie4 xie5")
            HangulAidText(hangul = null)
        }
    }
}
