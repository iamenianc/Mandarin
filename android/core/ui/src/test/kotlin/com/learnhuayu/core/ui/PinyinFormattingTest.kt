package com.learnhuayu.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinyinFormattingTest {

    @Test
    fun `syllables split tone numbered pinyin in order`() {
        val syllables = PinyinFormatting.syllables("dui4 bu5 qi3")

        assertThat(syllables)
            .containsExactly(
                PinyinSyllable("dui", 4),
                PinyinSyllable("bu", 5),
                PinyinSyllable("qi", 3),
            )
            .inOrder()
    }

    @Test
    fun `display string joins syllables with single spaces`() {
        assertThat(PinyinFormatting.displayString("  ni3   hao3  ")).isEqualTo("ni3 hao3")
    }

    @Test
    fun `tone marked vowels become tone numbers`() {
        assertThat(PinyinFormatting.displayString("nǐ hǎo")).isEqualTo("ni3 hao3")
        assertThat(PinyinFormatting.displayString("mā má mǎ mà ma")).isEqualTo("ma1 ma2 ma3 ma4 ma")
    }

    @Test
    fun `uppercase tone marked vowels keep their case`() {
        assertThat(PinyinFormatting.displayString("Nǐ Hǎo")).isEqualTo("Ni3 Hao3")
    }

    @Test
    fun `display never contains tone diacritics`() {
        val display = PinyinFormatting.displayString("nǐ hǎo mā")

        assertThat(display.any { it in 'ā'..'ǜ' }).isFalse()
    }

    @Test
    fun `hanzi are dropped from the display`() {
        assertThat(PinyinFormatting.displayString("\u4F60\u597D ni3 hao3")).isEqualTo("ni3 hao3")
    }

    @Test
    fun `umlaut vowel is preserved`() {
        assertThat(PinyinFormatting.displayString("lü4")).isEqualTo("lü4")
        assertThat(PinyinFormatting.displayString("nǚ")).isEqualTo("nü3")
    }

    @Test
    fun `neutral tone five is preserved`() {
        assertThat(PinyinFormatting.syllables("ma5").single().tone).isEqualTo(5)
    }

    @Test
    fun `syllables without tone keep a null tone`() {
        val syllable = PinyinFormatting.syllables("ma").single()

        assertThat(syllable).isEqualTo(PinyinSyllable("ma", null))
        assertThat(syllable.displayText).isEqualTo("ma")
    }

    @Test
    fun `apostrophes and hyphens separate syllables`() {
        assertThat(PinyinFormatting.displayString("xi1'an1")).isEqualTo("xi1 an1")
        assertThat(PinyinFormatting.displayString("ni3-hao3")).isEqualTo("ni3 hao3")
    }

    @Test
    fun `tone number accepts only one through five at the end`() {
        assertThat(PinyinFormatting.toneNumber("hao3")).isEqualTo(3)
        assertThat(PinyinFormatting.toneNumber("hao")).isNull()
        assertThat(PinyinFormatting.toneNumber("hao6")).isNull()
        assertThat(PinyinFormatting.toneNumber("hao0")).isNull()
        assertThat(PinyinFormatting.toneNumber("ha3o")).isNull()
    }

    @Test
    fun `blank input yields an empty display`() {
        assertThat(PinyinFormatting.displayString("")).isEmpty()
        assertThat(PinyinFormatting.displayString("   ")).isEmpty()
        assertThat(PinyinFormatting.displayString("\u4F60\u597D")).isEmpty()
    }
}
