package com.learnhuayu.core.audio.pcm

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PcmMathTest {

    @Test
    fun `rms of silence is zero`() {
        assertThat(PcmMath.rms(ShortArray(64))).isEqualTo(0f)
    }

    @Test
    fun `rms of a full scale square wave is close to one`() {
        val samples = ShortArray(64) { index -> if (index % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE }

        assertThat(PcmMath.rms(samples)).isWithin(0.001f).of(1f)
    }

    @Test
    fun `rms of a constant half scale signal is half`() {
        val samples = ShortArray(64) { 16384 }

        assertThat(PcmMath.rms(samples)).isWithin(0.001f).of(0.5f)
    }

    @Test
    fun `peak of silence is zero and peak of a full scale sample is close to one`() {
        assertThat(PcmMath.peak(ShortArray(8))).isEqualTo(0f)
        assertThat(PcmMath.peak(shortArrayOf(0, -32768, 1))).isWithin(0.001f).of(1f)
    }

    @Test
    fun `zero crossing rate of an alternating signal is one`() {
        val samples = ShortArray(64) { index -> if (index % 2 == 0) 100 else -100 }

        assertThat(PcmMath.zeroCrossingRate(samples)).isWithin(0.001f).of(1f)
    }

    @Test
    fun `zero crossing rate of a constant signal is zero`() {
        assertThat(PcmMath.zeroCrossingRate(ShortArray(64) { 100 })).isEqualTo(0f)
    }

    @Test
    fun `zero crossing rate of a ramp with one crossing counts one event`() {
        val samples = shortArrayOf(-3, -2, -1, 1, 2, 3)

        assertThat(PcmMath.zeroCrossingRate(samples)).isWithin(0.001f).of(0.2f)
    }
}
