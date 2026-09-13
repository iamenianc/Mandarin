package com.learnhuayu.core.model

data class ContentItem(
    val id: String,
    val type: ContentItemType,
    val source: ContentSource,
    val meaning: String,
    val audioAssetRef: String,
    val pinyin: String,
    val hangul: String? = null,
    val targetTones: List<Int>,
)

enum class ContentItemType {
    PHRASE,
    WORD,
    MINIMAL_PAIR,
    DIALOGUE,
}

enum class ContentSource {
    BUNDLED,
    GENERATED,
}
