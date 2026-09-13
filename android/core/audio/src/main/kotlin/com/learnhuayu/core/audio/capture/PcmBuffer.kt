package com.learnhuayu.core.audio.capture

internal class PcmBuffer(initialCapacity: Int = 24_000) {

    private var samples = ShortArray(initialCapacity.coerceAtLeast(1))

    var size: Int = 0
        private set

    fun append(source: ShortArray, offset: Int, count: Int) {
        require(offset >= 0 && count >= 0 && offset + count <= source.size) {
            "offset and count must describe a slice of the source"
        }
        ensureCapacity(size + count)
        System.arraycopy(source, offset, samples, size, count)
        size += count
    }

    fun clear() {
        size = 0
    }

    fun snapshot(): ShortArray = samples.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= samples.size) return
        var capacity = samples.size
        while (capacity < required) capacity *= 2
        samples = samples.copyOf(capacity)
    }
}
