package com.learnhuayu.core.audio.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PcmBufferTest {

    @Test
    fun `grows while appending and snapshots the buffered samples`() {
        val buffer = PcmBuffer(initialCapacity = 2)

        buffer.append(shortArrayOf(1, 2, 3, 4), offset = 0, count = 4)

        assertThat(buffer.size).isEqualTo(4)
        assertArrayEquals(shortArrayOf(1, 2, 3, 4), buffer.snapshot())
    }

    @Test
    fun `appends a slice of a larger frame`() {
        val buffer = PcmBuffer(initialCapacity = 1)

        buffer.append(shortArrayOf(9, 9, 5, 6, 9), offset = 2, count = 2)
        buffer.append(shortArrayOf(7), offset = 0, count = 1)

        assertArrayEquals(shortArrayOf(5, 6, 7), buffer.snapshot())
    }

    @Test
    fun `clear resets the size but keeps the buffer usable`() {
        val buffer = PcmBuffer(initialCapacity = 4)
        buffer.append(shortArrayOf(1, 2), offset = 0, count = 2)

        buffer.clear()
        buffer.append(shortArrayOf(3), offset = 0, count = 1)

        assertThat(buffer.size).isEqualTo(1)
        assertArrayEquals(shortArrayOf(3), buffer.snapshot())
    }
}
