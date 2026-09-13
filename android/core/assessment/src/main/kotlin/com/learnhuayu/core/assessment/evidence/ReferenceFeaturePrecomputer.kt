package com.learnhuayu.core.assessment.evidence

import com.learnhuayu.core.assessment.PcmAudio
import com.learnhuayu.core.assessment.dsp.AcousticFeatureExtractor
import com.learnhuayu.core.assessment.dsp.BaselineAcousticFeatureExtractor
import com.learnhuayu.core.assessment.segment.SyllableBoundary
import com.learnhuayu.core.assessment.segment.SyllableSegmenter

class ReferenceFeaturePrecomputer(
    private val extractor: AcousticFeatureExtractor = BaselineAcousticFeatureExtractor(),
) {

    fun precompute(pcm: PcmAudio, syllables: List<SyllableBoundary>): ReferenceToneFeatures? {
        if (syllables.isEmpty()) return null
        val contour = extractor.extract(pcm) ?: return null
        val spans = SyllableSegmenter.segment(contour, syllables) ?: return null
        if (spans.size != syllables.size) return null

        return ReferenceToneFeatures(
            sampleRateHz = contour.sampleRateHz,
            frameLengthMs = contour.frameLengthMs,
            hopMs = contour.hopMs,
            frames = contour.frames.map { frame ->
                ReferenceFrame(
                    timeMs = frame.timeMs,
                    f0Hz = frame.f0Hz,
                    clarity = frame.clarity,
                    voiced = frame.voiced,
                    rms = frame.rms,
                )
            },
            syllables = syllables,
        )
    }
}
