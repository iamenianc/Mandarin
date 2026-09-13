package com.learnhuayu.core.assessment.tone

object ToneNumber {
    const val TONE_1 = 1
    const val TONE_2 = 2
    const val TONE_3 = 3
    const val TONE_4 = 4
    const val NEUTRAL = 5

    val all: List<Int> = listOf(TONE_1, TONE_2, TONE_3, TONE_4, NEUTRAL)

    fun isValid(number: Int): Boolean = number in TONE_1..NEUTRAL
}
