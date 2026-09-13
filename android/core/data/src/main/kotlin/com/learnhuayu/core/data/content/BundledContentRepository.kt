package com.learnhuayu.core.data.content

import com.learnhuayu.core.model.ContentItem

interface BundledContentRepository {
    suspend fun modules(): List<ContentModule>

    suspend fun module(moduleId: String): ContentModule?

    suspend fun contentItem(contentItemId: String): ContentItem?

    suspend fun contentItems(contentItemIds: List<String>): List<ContentItem>

    fun audioAssetPath(audioAssetRef: String): String
}
