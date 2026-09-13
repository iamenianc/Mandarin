package com.learnhuayu.core.audio.vad

import com.learnhuayu.core.audio.pcm.PcmMath

class EnergyZeroCrossingEndOfSpeechDetector(
    private val sampleRateHz: Int,
    private val config: EnergyZeroCrossingConfig = EnergyZeroCrossingConfig(),
) : EndOfSpeechDetector {

    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
    }

    private var samplesProcessed = 0L
    private var speechRunStartSample = 0L
    private var silenceRunStartSample = 0L
    private var consecutiveSpeechFrames = 0
    private var consecutiveSilenceFrames = 0
    private var inSpeech = false

    override fun accept(frame: ShortArray, length: Int): VadEvent? {
        require(length in 0..frame.size) { "length must be within the frame" }
        val frameStartSample = samplesProcessed
        val speech = isSpeechFrame(frame, length)
        samplesProcessed += length

        if (inSpeech) {
            if (speech) {
                consecutiveSilenceFrames = 0
                return null
            }
            if (consecutiveSilenceFrames == 0) silenceRunStartSample = frameStartSample
            consecutiveSilenceFrames++
            if (consecutiveSilenceFrames >= config.silenceHangoverFrames) {
                inSpeech = false
                consecutiveSpeechFrames = 0
                consecutiveSilenceFrames = 0
                return VadEvent.SpeechEnded(msAt(silenceRunStartSample))
            }
            return null
        }

        if (!speech) {
            consecutiveSpeechFrames = 0
            return null
        }
        if (consecutiveSpeechFrames == 0) speechRunStartSample = frameStartSample
        consecutiveSpeechFrames++
        if (consecutiveSpeechFrames < config.minSpeechFrames) return null
        inSpeech = true
        consecutiveSilenceFrames = 0
        return VadEvent.SpeechStarted(msAt(speechRunStartSample))
    }

    override fun reset() {
        samplesProcessed = 0L
        speechRunStartSample = 0L
        silenceRunStartSample = 0L
        consecutiveSpeechFrames = 0
        consecutiveSilenceFrames = 0
        inSpeech = false
    }

    private fun msAt(sampleIndex: Long): Long = sampleIndex * 1000L / sampleRateHz

    private fun isSpeechFrame(frame: ShortArray, length: Int): Boolean {
        if (length <= 0) return false
        val rms = PcmMath.rms(frame, length)
        if (rms >= config.strongEnergyThresholdRms) return true
        if (rms < config.energyThresholdRms) return false
        return PcmMath.zeroCrossingRate(frame, length) <= config.maxZeroCrossingRate
    }
}
