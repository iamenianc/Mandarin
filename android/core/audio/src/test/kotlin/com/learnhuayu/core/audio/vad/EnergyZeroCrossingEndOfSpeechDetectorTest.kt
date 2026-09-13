package com.learnhuayu.core.audio.vad

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sin

class EnergyZeroCrossingEndOfSpeechDetectorTest {

    private val sampleRateHz = 1_000
    private val frameSize = 10

    private fun detector(
        config: EnergyZeroCrossingConfig = EnergyZeroCrossingConfig(
            minSpeechFrames = 2,
            silenceHangoverFrames = 2,
        ),
    ): EnergyZeroCrossingEndOfSpeechDetector = EnergyZeroCrossingEndOfSpeechDetector(sampleRateHz = sampleRateHz, config = config)

    private fun silence(): ShortArray = ShortArray(frameSize)

    private fun tone(amplitude: Int = 20_000): ShortArray = ShortArray(frameSize) { index ->
        (sin(2.0 * Math.PI * 100.0 * index / sampleRateHz) * amplitude).toInt().toShort()
    }

    private fun alternating(amplitude: Int): ShortArray = ShortArray(frameSize) { index -> if (index % 2 == 0) amplitude.toShort() else (-amplitude).toShort() }

    private fun noise(amplitude: Int): ShortArray {
        var state = 12_345
        return ShortArray(frameSize) {
            state = state * 1_103_515_245 + 12_345
            val value = (state shr 16) % (2 * amplitude) - amplitude
            value.toShort()
        }
    }

    @Test
    fun `silence produces no events`() {
        val detector = detector()

        repeat(10) {
            assertThat(detector.accept(silence())).isNull()
        }
    }

    @Test
    fun `tone burst produces speech started then speech ended`() {
        val detector = detector()
        val events = mutableListOf<VadEvent?>()

        repeat(4) { events += detector.accept(tone()) }
        repeat(2) { events += detector.accept(silence()) }

        assertThat(events[0]).isNull()
        assertThat(events[1]).isEqualTo(VadEvent.SpeechStarted(atMs = 0))
        assertThat(events[2]).isNull()
        assertThat(events[3]).isNull()
        assertThat(events[4]).isNull()
        assertThat(events[5]).isEqualTo(VadEvent.SpeechEnded(atMs = 40))
    }

    @Test
    fun `speech shorter than the minimum frame count produces no event`() {
        val detector = detector()

        assertThat(detector.accept(tone())).isNull()
        repeat(5) {
            assertThat(detector.accept(silence())).isNull()
        }
    }

    @Test
    fun `strong broadband noise starts speech`() {
        val detector = detector()

        assertThat(detector.accept(noise(amplitude = 20_000))).isNull()
        assertThat(detector.accept(noise(amplitude = 20_000)))
            .isEqualTo(VadEvent.SpeechStarted(atMs = 0))
    }

    @Test
    fun `moderate energy with a high zero crossing rate is not speech`() {
        val detector = detector()

        repeat(4) {
            assertThat(detector.accept(alternating(amplitude = 3_000))).isNull()
        }
    }

    @Test
    fun `quiet frames are not speech`() {
        val detector = detector()

        repeat(4) {
            assertThat(detector.accept(noise(amplitude = 100))).isNull()
        }
    }

    @Test
    fun `reset clears an unfinished speech run`() {
        val detector = detector()

        assertThat(detector.accept(tone())).isNull()
        detector.reset()

        assertThat(detector.accept(tone())).isNull()
        assertThat(detector.accept(tone())).isEqualTo(VadEvent.SpeechStarted(atMs = 0))
    }

    @Test
    fun `empty frames are ignored`() {
        val detector = detector()

        assertThat(detector.accept(ShortArray(0), length = 0)).isNull()
        assertThat(detector.accept(silence(), length = 0)).isNull()
    }

    @Test
    fun `default config requires three speech frames to start speech`() {
        val detector = EnergyZeroCrossingEndOfSpeechDetector(sampleRateHz = sampleRateHz)

        assertThat(detector.accept(tone())).isNull()
        assertThat(detector.accept(tone())).isNull()
        assertThat(detector.accept(tone())).isEqualTo(VadEvent.SpeechStarted(atMs = 0))
    }
}
