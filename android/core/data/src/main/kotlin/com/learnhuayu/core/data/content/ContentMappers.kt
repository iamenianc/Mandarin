package com.learnhuayu.core.data.content

import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentItemType
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec

internal fun LessonDto.toLessonSpec(): LessonSpec = LessonSpec(
    id = id,
    moduleId = moduleId,
    title = title,
    level = level,
    topic = topic,
    contentItemIds = contentItemIds,
)

internal fun LessonDto.toPracticeSpec(): PracticeSpec = PracticeSpec(
    id = id,
    moduleId = moduleId,
    title = title,
    contentItemIds = contentItemIds,
)

internal fun ContentItemDto.toModel(): ContentItem = ContentItem(
    id = id,
    type = type.toContentItemType(),
    source = source.toContentSource(),
    meaning = meaning,
    audioAssetRef = audioAssetRef,
    pinyin = pinyin,
    hangul = hangul,
    targetTones = targetTones,
)

private fun String.toContentItemType(): ContentItemType = when (this) {
    "word" -> ContentItemType.WORD
    "phrase" -> ContentItemType.PHRASE
    "minimalPair" -> ContentItemType.MINIMAL_PAIR
    "dialogue" -> ContentItemType.DIALOGUE
    else -> throw ContentAssetException("Unknown content item type: $this")
}

private fun String.toContentSource(): ContentSource = when (this) {
    "bundled" -> ContentSource.BUNDLED
    "generated" -> ContentSource.GENERATED
    else -> throw ContentAssetException("Unknown content source: $this")
}
