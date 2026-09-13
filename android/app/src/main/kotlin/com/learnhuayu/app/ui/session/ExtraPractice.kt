package com.learnhuayu.app.ui.session

import com.learnhuayu.core.ai.ExerciseItemType
import com.learnhuayu.core.ai.GeneratedExercise
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource

/** How many extra items one WF-8 call requests (WF-8, ADR 0010). */
const val EXTRA_GENERATION_COUNT = 5

/** Client-side timeout for one WF-8 call; a timeout falls back to the bundled set. */
const val EXTRA_GENERATION_TIMEOUT_MS = 10_000L

/**
 * Validates runtime-generated exercises (WF-8) before they join a session (NFR-10,
 * ADR 0010). Invalid items are dropped: blank pinyin or meaning, hanzi in the pinyin or
 * meaning, pinyin syllables without tone numbers, malformed tone lists, and duplicates
 * of session items or of each other (by pinyin, compared case- and tone-sensitively).
 * Admitted items are labeled [ContentSource.GENERATED] with stable per-session ids.
 */
internal object ExtraPracticeAdmission {

    /** The requested item type follows the practice's most common bundled item type. */
    fun itemTypeFor(items: List<ContentItem>): ExerciseItemType {
        val common = items.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key
            ?: ContentItemType.WORD
        return ExerciseItemType.valueOf(common.name)
    }

    /** Target units are the practice pinyin values plus the practiced tones as `toneN`. */
    fun targetUnitsFor(items: List<ContentItem>): List<String> {
        val pinyin = items.map { it.pinyin.trim() }.filter { it.isNotEmpty() }.distinct()
        val tones = items.flatMap { it.targetTones }.distinct().sorted().map { "tone$it" }
        return pinyin + tones
    }

    fun admit(
        exercises: List<GeneratedExercise>,
        existing: List<ContentItem>,
        specId: String,
        startSequence: Int,
    ): List<ContentItem> {
        val seen = existing.map { it.pinyin.trim() }.toSet()
        val admitted = mutableListOf<ContentItem>()
        var sequence = startSequence
        for (exercise in exercises) {
            val pinyin = exercise.pinyin.trim()
            if (pinyin.isEmpty() || exercise.meaning.isBlank()) continue
            if (containsHanzi(pinyin) || containsHanzi(exercise.meaning)) continue
            if (!pinyinHasToneNumbers(pinyin)) continue
            if (exercise.targetTones.isEmpty() || exercise.targetTones.any { it !in 1..5 }) continue
            if (pinyin in seen || admitted.any { it.pinyin == pinyin }) continue
            sequence += 1
            admitted += ContentItem(
                id = "extra-$specId-$sequence",
                type = ContentItemType.valueOf(exercise.type.name),
                source = ContentSource.GENERATED,
                meaning = exercise.meaning.trim(),
                audioAssetRef = "",
                pinyin = pinyin,
                hangul = exercise.hangul?.takeIf { it.isNotBlank() },
                targetTones = exercise.targetTones,
            )
        }
        return admitted
    }

    /** Every whitespace-separated pinyin syllable must end in a tone number (1-5). */
    fun pinyinHasToneNumbers(pinyin: String): Boolean {
        val syllables = pinyin.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (syllables.isEmpty()) return false
        return syllables.all { syllable ->
            syllable.length >= 2 &&
                syllable.last() in '1'..'5' &&
                syllable.dropLast(1).any { it.isLetter() }
        }
    }

    fun containsHanzi(value: String): Boolean {
        val codePoints = value.codePoints().iterator()
        while (codePoints.hasNext()) {
            val codePoint = codePoints.nextInt()
            if (
                codePoint in 0x3400..0x4DBF ||
                codePoint in 0x4E00..0x9FFF ||
                codePoint in 0x20000..0x2EBEF
            ) {
                return true
            }
        }
        return false
    }
}
