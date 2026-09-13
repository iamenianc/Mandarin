package com.learnhuayu.core.assessment.align

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class DtwAlignerTest {

    private val aligner = DtwAligner()

    private fun ramp(size: Int, step: Float = 0.05f): List<FloatArray> = List(size) { index -> floatArrayOf(index * step) }

    @Test
    fun `alignment of identical contours runs along the diagonal`() {
        val reference = ramp(size = 24)
        val attempt = ramp(size = 24)

        val alignment = aligner.align(reference, attempt)

        assertThat(alignment).isNotNull()
        assertThat(alignment!!.normalizedDistance).isLessThan(1e-6f)
        assertThat(alignment.path.first()).isEqualTo(DtwStep(0, 0))
        assertThat(alignment.path.last()).isEqualTo(DtwStep(23, 23))
    }

    @Test
    fun `alignment tolerates additive noise and stays near the diagonal`() {
        val reference = ramp(size = 24)
        val random = Random(42)
        val attempt = reference.map { frame -> floatArrayOf(frame[0] + (random.nextFloat() - 0.5f) * 0.02f) }

        val alignment = aligner.align(reference, attempt)

        assertThat(alignment).isNotNull()
        assertThat(alignment!!.normalizedDistance).isLessThan(0.01f)
        val maximumDeviation = alignment.path.maxOf { abs(it.referenceIndex - it.attemptIndex) }
        assertThat(maximumDeviation).isAtMost(3)
    }

    @Test
    fun `alignment tolerates a leading temporal offset`() {
        val reference = ramp(size = 20)
        val lead = 4
        val attempt = List(lead) { floatArrayOf(reference[0][0]) } + reference

        val alignment = aligner.align(reference, attempt)

        assertThat(alignment).isNotNull()
        assertThat(alignment!!.path.first()).isEqualTo(DtwStep(0, 0))
        assertThat(alignment.path.last()).isEqualTo(DtwStep(19, 23))
        assertThat(alignment.attemptRangeFor(0..0)!!.first).isEqualTo(0)
        assertThat(alignment.attemptRangeFor(19..19)!!.last).isEqualTo(23)

        for (referenceIndex in reference.indices) {
            val mapped = alignment.attemptRangeFor(referenceIndex..referenceIndex)
            assertThat(mapped).isNotNull()
            assertThat(mapped!!.first).isAtLeast(referenceIndex)
        }
    }
}
