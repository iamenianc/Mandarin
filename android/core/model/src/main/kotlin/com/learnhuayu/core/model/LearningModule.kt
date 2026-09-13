package com.learnhuayu.core.model

interface LearningModule {
    val id: String
    val title: String

    suspend fun lessons(): List<LessonSpec>

    suspend fun practices(): List<PracticeSpec>
}
