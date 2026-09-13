package com.learnhuayu.core.assessment.dsp

import com.learnhuayu.core.assessment.FrameConfig
import com.learnhuayu.core.assessment.PcmAudio

interface AcousticFeatureExtractor {
    fun extract(pcm: PcmAudio): FeatureContour?
}

class BaselineAcousticFeatureExtractor(
    private val f0Estimator: F0Estimator = AutocorrelationF0Estimator(),
    private val voicingDecider: VoicingDecider = EnergyClarityVoicingDecider(),
    private val loudnessEstimator: LoudnessEstimator = RmsLoudnessEstimator(),
    private val frameConfig: FrameConfig = FrameConfig(),
) : AcousticFeatureExtractor {

    override fun extract(pcm: PcmAudio): FeatureContour? {
        if (pcm.samples.isEmpty()) return null
        if (!pcm.isFinite()) return null

        val frameLengthSamples = frameConfig.frameLengthSamples(pcm.sampleRateHz)
        if (frameLengthSamples <= 0 || pcm.samples.size < frameLengthSamples) return null

        val hopSamples = frameConfig.hopSamples(pcm.sampleRateHz).coerceAtLeast(1)
        val frames = ArrayList<FrameFeature>()
        var start = 0
        var index = 0
        while (start + frameLengthSamples <= pcm.samples.size) {
            val frame = pcm.samples.copyOfRange(start, start + frameLengthSamples)
            val rms = loudnessEstimator.rms(frame)
            val estimate = f0Estimator.estimate(frame, pcm.sampleRateHz)
            val voiced = voicingDecider.isVoiced(frame, rms, estimate, pcm.sampleRateHz)
            frames += FrameFeature(
                index = index,
                timeMs = start * 1000 / pcm.sampleRateHz,
                f0Hz = if (voiced) estimate.f0Hz else null,
                clarity = estimate.clarity,
                voiced = voiced,
                rms = rms,
            )
            start += hopSamples
            index++
        }

        if (frames.isEmpty()) return null
        return FeatureContour(
            frames = frames,
            sampleRateHz = pcm.sampleRateHz,
            frameLengthMs = frameConfig.frameLengthMs,
            hopMs = frameConfig.hopMs,
        )
    }
}
