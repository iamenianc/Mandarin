package com.learnhuayu.app.ui.session

import com.learnhuayu.app.audio.ReferencePcmDecoder
import com.learnhuayu.core.ai.AcousticEvidence
import com.learnhuayu.core.ai.AcousticSyllable
import com.learnhuayu.core.assessment.ToneAssessmentPipeline
import com.learnhuayu.core.assessment.dsp.AcousticFeatureExtractor
import com.learnhuayu.core.assessment.evidence.ReferenceFeaturePrecomputer
import com.learnhuayu.core.assessment.evidence.ReferenceToneFeatures
import com.learnhuayu.core.assessment.evidence.ToneEvidence
import com.learnhuayu.core.assessment.segment.SyllableAutoSegmenter
import com.learnhuayu.core.assessment.tone.ToneNumber
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.data.content.BundledContentRepository
import com.learnhuayu.core.model.ContentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import com.learnhuayu.core.assessment.PcmAudio as AssessmentPcmAudio

/**
 * Measures the learner's attempt against the bundled reference clip on device (ADR 0014).
 *
 * The bundled reference carries no timestamps, so its syllables are recovered from the clip's
 * own energy contour with [SyllableAutoSegmenter] and the expected tones taken from the item
 * pinyin. The precomputed [ReferenceToneFeatures] are cached per content item for the process
 * lifetime, so only the first attempt on an item decodes and measures the reference.
 *
 * Every step degrades to `null`: a missing clip, an undecodable format, a decode or precompute
 * failure, an empty or mismatched pinyin, or an unvoiced attempt all return `null`, and the
 * caller keeps the audio-only WF-1 request.
 */
@Singleton
class AssessmentAttemptEvidenceSource @Inject constructor(
    private val contentRepository: BundledContentRepository,
    private val referenceClipReader: ReferenceClipReader,
    private val referencePcmDecoder: ReferencePcmDecoder,
    private val extractor: AcousticFeatureExtractor,
    private val precomputer: ReferenceFeaturePrecomputer,
    private val pipeline: ToneAssessmentPipeline,
) : AttemptEvidenceSource {

    private val cache = ConcurrentHashMap<String, ReferenceToneFeatures>()

    override suspend fun evidenceFor(item: ContentItem, attempt: PcmAudio): AcousticEvidence? = withContext(Dispatchers.Default) {
        val syllables = splitPinyin(item.pinyin)
        val tones = expectedTones(item, syllables) ?: return@withContext null
        val features = referenceFeatures(item, tones) ?: return@withContext null
        val evidence = pipeline.assess(attempt.toAssessmentPcm(), features) ?: return@withContext null
        evidence.toAcousticEvidence(item.pinyin, syllables)
    }

    private suspend fun referenceFeatures(item: ContentItem, tones: List<Int>): ReferenceToneFeatures? {
        cache[item.id]?.let { return it }
        val assetPath = contentRepository.audioAssetPath(item.audioAssetRef)
        val clip = referenceClipReader.read(assetPath) ?: return null
        val decoded = referencePcmDecoder.decode(clip) ?: return null
        val pcm = decoded.toAssessmentPcm()
        val contour = extractor.extract(pcm) ?: return null
        val boundaries = SyllableAutoSegmenter.segment(contour, tones) ?: return null
        val features = precomputer.precompute(pcm, boundaries) ?: return null
        cache[item.id] = features
        return features
    }

    private fun splitPinyin(pinyin: String): List<String> = pinyin.trim().split(WHITESPACE).filter { it.isNotBlank() }

    private fun expectedTones(item: ContentItem, syllables: List<String>): List<Int>? {
        if (syllables.isEmpty()) return null
        val parsed = syllables.map { trailingTone(it) }
        val fromPinyin = if (parsed.all { it != null }) parsed.filterNotNull() else null
        return when {
            fromPinyin != null && (item.targetTones.isEmpty() || item.targetTones.size == fromPinyin.size) -> fromPinyin
            fromPinyin == null && item.targetTones.size == syllables.size -> item.targetTones
            else -> null
        }
    }

    private fun trailingTone(syllable: String): Int? = syllable.lastOrNull()?.digitToIntOrNull()?.takeIf { ToneNumber.isValid(it) }

    private fun PcmAudio.toAssessmentPcm(): AssessmentPcmAudio = AssessmentPcmAudio(
        samples = FloatArray(samples.size) { index -> samples[index] / SHORT_MAX },
        sampleRateHz = sampleRateHz,
    )

    private fun ToneEvidence.toAcousticEvidence(phrase: String, syllables: List<String>): AcousticEvidence? {
        val mapped = ArrayList<AcousticSyllable>(this.syllables.size)
        for (observation in this.syllables) {
            val pinyin = syllables.getOrNull(observation.index) ?: return null
            mapped += AcousticSyllable(
                pinyin = pinyin,
                expectedTone = observation.expectedTone,
                observed = observation.tone?.toString(),
                direction = observation.contourDirection.name.lowercase(Locale.ROOT),
                voicingRatio = observation.voicingRatio.toDouble(),
            )
        }
        return AcousticEvidence(phrase = phrase, syllables = mapped)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        const val SHORT_MAX = 32768f
    }
}
