package com.learnhuayu.app.ui.session

import android.content.Context
import com.learnhuayu.core.ai.AcousticEvidence
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.model.ContentItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/**
 * Reads the bundled reference clip bytes for one content item so WF-1 can send them as the
 * model clip (ADR 0005). The generated clips are build-time inputs and are not committed
 * (ADR 0006), so a missing asset returns `null` and the caller falls back to offline drills.
 */
interface ReferenceClipReader {
    suspend fun read(audioAssetPath: String): AudioClip?
}

class AssetReferenceClipReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReferenceClipReader {

    override suspend fun read(audioAssetPath: String): AudioClip? {
        val format = detectAudioFormat(audioAssetPath) ?: return null
        val bytes = try {
            context.assets.open(audioAssetPath).use { stream -> stream.readBytes() }
        } catch (error: FileNotFoundException) {
            return null
        } catch (error: IOException) {
            return null
        }
        if (bytes.isEmpty()) return null
        return AudioClip(bytes = bytes, format = format)
    }
}

/** Detects the clip format from its asset extension; unknown extensions are treated as missing. */
fun detectAudioFormat(audioAssetPath: String): AudioFormat? = when (
    audioAssetPath.substringAfterLast('.', "").lowercase(Locale.ROOT)
) {
    "wav" -> AudioFormat.WAV
    "mp3" -> AudioFormat.MP3
    "m4a" -> AudioFormat.M4A
    "ogg" -> AudioFormat.OGG
    "webm" -> AudioFormat.WEBM
    "flac" -> AudioFormat.FLAC
    else -> null
}

/**
 * Turns one recorded attempt into the compact, versioned per-syllable evidence WF-1 accepts
 * (ADR 0014). [AssessmentAttemptEvidenceSource] measures the attempt against the bundled
 * reference clip on device: it decodes the clip, recovers its syllables from the energy
 * contour, precomputes the reference features, runs the `:core:assessment` pipeline, and maps
 * the result to `:core:ai.AcousticEvidence`. Any missing or failed step returns `null` so the
 * request degrades to the audio-only form.
 */
interface AttemptEvidenceSource {
    suspend fun evidenceFor(item: ContentItem, attempt: PcmAudio): AcousticEvidence?
}
