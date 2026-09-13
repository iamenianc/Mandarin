package com.learnhuayu.core.assessment

import com.learnhuayu.core.assessment.align.DtwAligner
import com.learnhuayu.core.assessment.dsp.AcousticFeatureExtractor
import com.learnhuayu.core.assessment.dsp.BaselineAcousticFeatureExtractor
import com.learnhuayu.core.assessment.evidence.ReferenceToneFeatures
import com.learnhuayu.core.assessment.evidence.SyllableToneObservation
import com.learnhuayu.core.assessment.evidence.ToneEvidence
import com.learnhuayu.core.assessment.normalize.NormalizedContour
import com.learnhuayu.core.assessment.normalize.NormalizedFrame
import com.learnhuayu.core.assessment.normalize.SpeakerRelativeNormalizer
import com.learnhuayu.core.assessment.segment.SyllableSegmenter
import com.learnhuayu.core.assessment.tone.SyllableStats
import com.learnhuayu.core.assessment.tone.ToneClassifier
import kotlin.math.ln
import kotlin.math.round

class ToneAssessmentPipeline(
    private val extractor: AcousticFeatureExtractor = BaselineAcousticFeatureExtractor(),
    private val normalizer: SpeakerRelativeNormalizer = SpeakerRelativeNormalizer(),
    private val classifier: ToneClassifier = ToneClassifier(),
    private val aligner: DtwAligner = DtwAligner(),
) {

    fun assess(attempt: PcmAudio, reference: ReferenceToneFeatures): ToneEvidence? {
        val referenceContour = reference.toContour()
        val referenceSpans = SyllableSegmenter.segment(referenceContour, reference.syllables) ?: return null

        val attemptContour = extractor.extract(attempt) ?: return null
        val normalizedReference = normalizer.normalize(referenceContour) ?: return null
        val normalizedAttempt = normalizer.normalize(attemptContour) ?: return null

        val alignment = aligner.align(
            reference = normalizedReference.toAlignmentFeatures(),
            attempt = normalizedAttempt.toAlignmentFeatures(),
        ) ?: return null

        val mappedRanges = referenceSpans.map { span -> alignment.attemptRangeFor(span.frameRange) }
        val durations = mappedRanges.map { range -> durationMs(normalizedAttempt, range) }
        val positiveDurations = durations.filter { it > 0 }.sorted()
        val medianDurationMs = if (positiveDurations.isEmpty()) 1 else positiveDurations[positiveDurations.size / 2]

        val observations = referenceSpans.mapIndexed { index, span ->
            val frames = mappedRanges[index]?.let { normalizedAttempt.framesIn(it) } ?: emptyList()
            val voicedFrames = frames.filter { it.voiced && it.level != null }
            val voicingRatio = if (frames.isEmpty()) 0f else voicedFrames.size.toFloat() / frames.size
            val loudnessRatio = if (voicedFrames.isEmpty()) {
                0f
            } else {
                voicedFrames.map { it.loudnessRatio }.average().toFloat()
            }
            val durationRatio = durations[index].toFloat() / medianDurationMs
            val stats = buildStats(voicedFrames, voicingRatio, loudnessRatio, durationRatio)
            val classification = classifier.classify(stats)
            SyllableToneObservation(
                index = index,
                tone = classification.tone,
                expectedTone = span.boundary.expectedTone,
                match = classification.tone != null && classification.tone == span.boundary.expectedTone,
                contourDirection = classification.direction,
                voicingRatio = roundToHundredths(voicingRatio),
                loudnessRatio = roundToHundredths(loudnessRatio),
            )
        }

        return ToneEvidence(syllables = observations)
    }

    private fun durationMs(contour: NormalizedContour, range: IntRange?): Int {
        if (range == null || range.isEmpty()) return 0
        val first = contour.frames[range.first.coerceIn(0, contour.frames.size - 1)]
        val last = contour.frames[range.last.coerceIn(0, contour.frames.size - 1)]
        return last.timeMs - first.timeMs + contour.hopMs
    }

    private fun buildStats(
        voicedFrames: List<NormalizedFrame>,
        voicingRatio: Float,
        loudnessRatio: Float,
        durationRatio: Float,
    ): SyllableStats {
        val levels = voicedFrames.mapNotNull { it.level }
        if (levels.isEmpty()) {
            return SyllableStats(
                onsetLevel = null,
                offsetLevel = null,
                minimumLevel = null,
                minimumPosition = 0f,
                voicedFrames = 0,
                voicingRatio = voicingRatio,
                loudnessRatio = loudnessRatio,
                durationRatio = durationRatio,
            )
        }

        val edgeSize = (levels.size / 3).coerceAtLeast(1)
        val onset = median(levels.take(edgeSize))
        val offset = median(levels.takeLast(edgeSize))

        var minimum = levels.first()
        var minimumPosition = 0f
        if (levels.size > 1) {
            for (index in levels.indices) {
                if (levels[index] < minimum) {
                    minimum = levels[index]
                    minimumPosition = index.toFloat() / (levels.size - 1)
                }
            }
        }

        return SyllableStats(
            onsetLevel = onset,
            offsetLevel = offset,
            minimumLevel = minimum,
            minimumPosition = minimumPosition,
            voicedFrames = levels.size,
            voicingRatio = voicingRatio,
            loudnessRatio = loudnessRatio,
            durationRatio = durationRatio,
        )
    }

    private fun NormalizedContour.toAlignmentFeatures(): List<FloatArray> = frames.map { frame ->
        floatArrayOf(
            frame.level ?: UNVOICED_LEVEL,
            ln(frame.loudnessRatio + LOUDNESS_OFFSET).toFloat(),
            if (frame.voiced) 1f else 0f,
        )
    }

    private fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2f
        } else {
            sorted[middle]
        }
    }

    private fun roundToHundredths(value: Float): Float = round(value * 100f) / 100f

    private companion object {
        const val UNVOICED_LEVEL = 0.5f
        const val LOUDNESS_OFFSET = 0.1f
    }
}
