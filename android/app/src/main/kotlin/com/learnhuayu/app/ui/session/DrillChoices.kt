package com.learnhuayu.app.ui.session

import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.DrillMode

/**
 * Builds choice lists for the tap drills. Order and distractor selection are pure
 * functions of the spec's items, so the same item always produces the same choices and
 * the behavior is testable (no randomness, no clock, no map iteration order).
 */
object DrillChoices {

    const val CHOICE_COUNT = 4

    fun forItem(
        item: ContentItem,
        availableItems: List<ContentItem>,
        mode: DrillMode,
    ): List<String> {
        val correct = answerLabel(item, mode)
        val distractors = availableItems
            .filter { candidate -> candidate.id != item.id }
            .map { candidate -> answerLabel(candidate, mode) }
            .filter { label -> label != correct }
            .distinct()
            .sorted()
            .take(CHOICE_COUNT - 1)
        return (listOf(correct) + distractors).sorted()
    }

    /**
     * The expected on-screen answer for one item: the tone sequence for hear-and-name
     * (tones written as numbers, joined with `-` when longer than one; ADR 0012) and the
     * pinyin for listen-and-choose.
     */
    fun answerLabel(item: ContentItem, mode: DrillMode): String = when (mode) {
        DrillMode.HEAR_AND_NAME -> item.targetTones.joinToString("-")
        DrillMode.LISTEN_AND_CHOOSE, DrillMode.LESSON, DrillMode.SPEAK_AND_REPEAT -> item.pinyin
    }
}
