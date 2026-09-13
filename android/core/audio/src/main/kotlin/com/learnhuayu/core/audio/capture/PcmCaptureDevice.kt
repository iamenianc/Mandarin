package com.learnhuayu.core.audio.capture

interface PcmCaptureDevice {

    val sampleRateHz: Int

    val bufferSizeInShorts: Int

    fun start()

    fun read(buffer: ShortArray, offset: Int, size: Int): Int

    fun stop()

    fun release()
}
