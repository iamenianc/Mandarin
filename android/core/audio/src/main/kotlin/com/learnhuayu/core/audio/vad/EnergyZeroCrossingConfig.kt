package com.learnhuayu.core.audio.vad

data class EnergyZeroCrossingConfig(
    val energyThresholdRms: Float = 0.02f,
    val strongEnergyThresholdRms: Float = 0.10f,
    val maxZeroCrossingRate: Float = 0.35f,
    val minSpeechFrames: Int = 3,
    val silenceHangoverFrames: Int = 5,
) {
    init {
        require(energyThresholdRms > 0f) { "energyThresholdRms must be positive" }
        require(strongEnergyThresholdRms >= energyThresholdRms) {
            "strongEnergyThresholdRms must be at least energyThresholdRms"
        }
        require(maxZeroCrossingRate in 0f..1f) { "maxZeroCrossingRate must be within 0..1" }
        require(minSpeechFrames >= 1) { "minSpeechFrames must be positive" }
        require(silenceHangoverFrames >= 1) { "silenceHangoverFrames must be positive" }
    }
}
