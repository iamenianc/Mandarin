package com.learnhuayu.core.data.content

import com.learnhuayu.core.model.LearningModule
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec

data class ContentModule(
    override val id: String,
    override val title: String,
    val theme: String,
    val lessonSpecs: List<LessonSpec>,
    val practiceSpecs: List<PracticeSpec>,
) : LearningModule {
    override suspend fun lessons(): List<LessonSpec> = lessonSpecs

    override suspend fun practices(): List<PracticeSpec> = practiceSpecs
}
