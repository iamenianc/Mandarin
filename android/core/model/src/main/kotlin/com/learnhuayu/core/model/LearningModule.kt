package com.learnhuayu.core.model

interface LearningModule {
    val id: String
    val title: String

    fun lessons(): List<LessonSpec>

    fun practices(): List<PracticeSpec>
}
