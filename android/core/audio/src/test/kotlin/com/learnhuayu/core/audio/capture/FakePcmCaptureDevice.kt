package com.learnhuayu.core.audio.capture

class FakePcmCaptureDevice(
    override val sampleRateHz: Int = 1_000,
    override val bufferSizeInShorts: Int = 4,
) : PcmCaptureDevice {

    private val pending = ArrayDeque<ShortArray>()

    var startCount: Int = 0
        private set
    var stopped: Boolean = false
        private set
    var released: Boolean = false
        private set
    var readError: Int? = null
    var startError: String? = null

    fun enqueue(frame: ShortArray) {
        pending.addLast(frame)
    }

    override fun start() {
        startError?.let { throw IllegalStateException(it) }
        startCount++
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
        val next = pending.removeFirstOrNull()
        if (next != null) {
            val count = minOf(size, next.size)
            for (index in 0 until count) {
                buffer[offset + index] = next[index]
            }
            return count
        }
        readError?.let { return it }
        return 0
    }

    override fun stop() {
        stopped = true
    }

    override fun release() {
        released = true
    }
}
