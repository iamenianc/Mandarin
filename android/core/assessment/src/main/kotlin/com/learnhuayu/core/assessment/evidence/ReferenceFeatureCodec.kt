package com.learnhuayu.core.assessment.evidence

import kotlinx.serialization.json.Json

object ReferenceFeatureCodec {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun encode(features: ReferenceToneFeatures): String = json.encodeToString(ReferenceToneFeatures.serializer(), features)

    fun decode(text: String): ReferenceToneFeatures? = runCatching {
        json.decodeFromString(ReferenceToneFeatures.serializer(), text)
    }.getOrNull()
}
