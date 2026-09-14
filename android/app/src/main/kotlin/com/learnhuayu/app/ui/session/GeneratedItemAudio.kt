package com.learnhuayu.app.ui.session

import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.ai.SpeechSynthesisRequest
import com.learnhuayu.core.ai.SpeechSynthesisWorkflow
import com.learnhuayu.core.ai.SynthesizedSpeech
import com.learnhuayu.core.ai.WorkflowFailure
import com.learnhuayu.core.ai.WorkflowResult
import com.learnhuayu.core.model.ContentItem
import com.learnhuayu.core.model.ContentSource
import java.io.File

/** Recording-store label for synthesized generated-item reference clips (WF-3, FR-26). */
const val GENERATED_AUDIO_LABEL = "extra-reference"

/**
 * Silent default for generated-item voicing. Production injects the real WF-3 workflow;
 * callers that do not pass one keep the missing-clip behavior for generated items.
 */
internal object NoOpSpeechSynthesisWorkflow : SpeechSynthesisWorkflow {
    override suspend fun synthesize(request: SpeechSynthesisRequest): WorkflowResult<SynthesizedSpeech> = WorkflowResult.Failure(WorkflowFailure.BaseUrlMissing())
}

/** Generated items carry no bundled clip (`audioAssetRef = ""`) and need WF-3 voicing. */
internal fun ContentItem.needsGeneratedVoice(): Boolean = source == ContentSource.GENERATED

/** File extension for WF-3 bytes, mirroring the cache-file audio players. */
internal fun speechExtensionForContentType(contentType: String?): String = when {
    contentType?.contains("mpeg", ignoreCase = true) == true -> "mp3"
    contentType?.contains("ogg", ignoreCase = true) == true -> "ogg"
    contentType?.contains("mp4", ignoreCase = true) == true -> "m4a"
    contentType?.contains("m4a", ignoreCase = true) == true -> "m4a"
    else -> "wav"
}

/** WF-1 clip format matching the WF-3 content type. */
internal fun audioFormatForContentType(contentType: String?): AudioFormat = when (speechExtensionForContentType(contentType)) {
    "mp3" -> AudioFormat.MP3
    "ogg" -> AudioFormat.OGG
    "m4a" -> AudioFormat.M4A
    else -> AudioFormat.WAV
}

/**
 * Resolves the cache file for synthesized bytes next to the reserved recording-store file,
 * so the clip lives in the app cache in the same style as recordings. The reserved file
 * always ends in `.wav`; synthesized bytes may be another container, so a sibling with the
 * matching extension is used instead.
 */
internal fun generatedCacheFile(reserved: File, contentType: String?): File {
    val extension = speechExtensionForContentType(contentType)
    if (reserved.extension.equals(extension, ignoreCase = true)) return reserved
    val parent = reserved.parentFile ?: return reserved
    return File(parent, reserved.nameWithoutExtension + "." + extension)
}

/** One cached WF-3 reference clip: its playback path plus the WF-1 clip bytes. */
internal data class CachedGeneratedReference(
    val path: String,
    val clip: AudioClip,
)
