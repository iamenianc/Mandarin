package com.learnhuayu.core.ui

/**
 * One pinyin syllable split from a phrase, with its tone number when the input carried
 * one. Tone numbers are the display convention everywhere (ADR 0012).
 */
data class PinyinSyllable(
    val text: String,
    val tone: Int?,
) {
    val displayText: String
        get() = if (tone == null) text else "$text$tone"
}

/**
 * Parses and displays pinyin that is safe for the audio-first UI: tones are written as
 * numbers, never diacritics (ADR 0012), and hanzi are dropped (FR-17). Pure Kotlin, so
 * every rule is unit-testable.
 */
object PinyinFormatting {
    const val FIRST_TONE = 1
    const val NEUTRAL_TONE = 5

    private val SYLLABLE_SEPARATORS = Regex("[\\s'’\\-]+")

    fun syllables(pinyin: String): List<PinyinSyllable> = SYLLABLE_SEPARATORS.split(pinyin)
        .mapNotNull(::syllable)

    fun displayString(pinyin: String): String = syllables(pinyin).joinToString(" ") { it.displayText }

    fun toneNumber(syllable: String): Int? {
        val last = syllable.lastOrNull() ?: return null
        val tone = last.digitToIntOrNull() ?: return null
        return tone.takeIf { it in FIRST_TONE..NEUTRAL_TONE }
    }

    private fun syllable(raw: String): PinyinSyllable? {
        val cleaned = sanitize(raw)
        if (cleaned.isEmpty()) return null
        val explicitTone = toneNumber(cleaned)
        if (explicitTone != null) {
            val text = cleaned.dropLast(1)
            return text.takeIf { it.isNotEmpty() }?.let { PinyinSyllable(it, explicitTone) }
        }
        val (text, markedTone) = stripToneMarks(cleaned)
        return text.takeIf { it.isNotEmpty() }?.let { PinyinSyllable(it, markedTone) }
    }

    private fun sanitize(text: String): String {
        val builder = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (Character.charCount(codePoint) == 1) {
                val char = text[index]
                if (char in TONE_MARKED_VOWELS || char in PLAIN_PINYIN_CHARS) {
                    builder.append(char)
                }
            }
            index += Character.charCount(codePoint)
        }
        return builder.toString()
    }

    private fun stripToneMarks(text: String): Pair<String, Int?> {
        val builder = StringBuilder(text.length)
        var tone: Int? = null
        text.forEach { char ->
            val marked = TONE_MARKED_VOWELS[char]
            if (marked == null) {
                builder.append(char)
            } else {
                if (tone == null) tone = marked.tone
                builder.append(marked.plain)
            }
        }
        return builder.toString() to tone
    }

    private data class ToneMarkedVowel(
        val plain: Char,
        val tone: Int,
    )

    private val PLAIN_PINYIN_CHARS: Set<Char> = buildSet {
        addAll('a'..'z')
        addAll('A'..'Z')
        addAll('0'..'9')
        add('ü')
        add('Ü')
        add(':')
    }

    private val TONE_MARKED_VOWELS: Map<Char, ToneMarkedVowel> = buildMap {
        val plain = "aeiouü"
        val upperPlain = "AEIOUÜ"
        val marked = listOf("āēīōūǖ", "áéíóúǘ", "ǎěǐǒǔǚ", "àèìòùǜ")
        val markedUpper = listOf("ĀĒĪŌŪǕ", "ÁÉÍÓÚǗ", "ǍĚǏǑǓǙ", "ÀÈÌÒÙǛ")
        marked.forEachIndexed { index, vowels ->
            vowels.forEachIndexed { vowelIndex, char ->
                put(char, ToneMarkedVowel(plain[vowelIndex], index + 1))
            }
        }
        markedUpper.forEachIndexed { index, vowels ->
            vowels.forEachIndexed { vowelIndex, char ->
                put(char, ToneMarkedVowel(upperPlain[vowelIndex], index + 1))
            }
        }
    }
}
