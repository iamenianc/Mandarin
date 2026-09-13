package com.learnhuayu.core.data.content

import kotlinx.serialization.Serializable

@Serializable
internal data class ContentIndexDto(
    val version: Int = 1,
    val modules: List<ContentIndexModuleDto> = emptyList(),
)

@Serializable
internal data class ContentIndexModuleDto(
    val id: String,
    val title: String,
    val path: String,
    val index: String,
    val lessonCount: Int? = null,
    val itemCount: Int? = null,
)

@Serializable
internal data class ModuleIndexDto(
    val version: Int = 1,
    val moduleId: String,
    val module: String = "module.json",
    val lessons: String = "lessons.json",
    val items: String = "items.json",
)

@Serializable
internal data class ModuleDto(
    val id: String,
    val title: String,
    val theme: String,
    val lessonIds: List<String>,
)

@Serializable
internal data class LessonsFileDto(
    val moduleId: String,
    val lessons: List<LessonDto> = emptyList(),
)

@Serializable
internal data class LessonDto(
    val id: String,
    val moduleId: String,
    val title: String,
    val level: String,
    val topic: String,
    val kind: String,
    val contentItemIds: List<String>,
)

@Serializable
internal data class ItemsFileDto(
    val moduleId: String,
    val items: List<ContentItemDto> = emptyList(),
)

@Serializable
internal data class ContentItemDto(
    val id: String,
    val type: String,
    val source: String,
    val meaning: String,
    val pinyin: String,
    val targetTones: List<Int>,
    val audioAssetRef: String,
    val hangul: String? = null,
    val turns: List<ContentTurnDto>? = null,
)

@Serializable
internal data class ContentTurnDto(
    val speaker: String,
    val pinyin: String,
    val meaning: String,
    val targetTones: List<Int>,
)
