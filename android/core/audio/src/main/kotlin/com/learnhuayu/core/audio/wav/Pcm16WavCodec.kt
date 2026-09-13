package com.learnhuayu.core.audio.wav

import com.learnhuayu.core.audio.pcm.AudioSpec
import com.learnhuayu.core.audio.pcm.PcmAudio
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Pcm16WavCodec : WavCodec {

    override fun encode(audio: PcmAudio): ByteArray {
        val samples = audio.samples
        val dataSize = samples.size * AudioSpec.BYTES_PER_SAMPLE
        val format = WavFormat(sampleRateHz = audio.sampleRateHz)
        val buffer = ByteBuffer.allocate(HEADER_SIZE + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(FOURCC_RIFF)
        buffer.putInt(RIFF_SIZE_BASE + dataSize)
        buffer.put(FOURCC_WAVE)
        buffer.put(FOURCC_FMT)
        buffer.putInt(FMT_CHUNK_SIZE)
        buffer.putShort(WavFormat.FORMAT_PCM.toShort())
        buffer.putShort(format.channelCount.toShort())
        buffer.putInt(format.sampleRateHz)
        buffer.putInt(format.byteRate)
        buffer.putShort(format.blockAlign.toShort())
        buffer.putShort(format.bitsPerSample.toShort())
        buffer.put(FOURCC_DATA)
        buffer.putInt(dataSize)
        for (sample in samples) {
            buffer.putShort(sample)
        }
        return buffer.array()
    }

    override fun decode(bytes: ByteArray): PcmAudio {
        if (bytes.size < MINIMUM_SIZE) {
            throw WavFormatException("payload is too small to be a RIFF/WAVE file")
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (!buffer.matches(FOURCC_OFFSET, FOURCC_RIFF) || !buffer.matches(WAVE_OFFSET, FOURCC_WAVE)) {
            throw WavFormatException("payload is not a RIFF/WAVE file")
        }

        var format: WavFormat? = null
        var dataOffset = -1
        var dataSize = 0
        var position = RIFF_HEADER_SIZE
        while (position + CHUNK_HEADER_SIZE <= bytes.size) {
            val chunkId = String(bytes, position, FOURCC_RIFF.size, Charsets.US_ASCII)
            val declaredSize = buffer.getInt(position + FOURCC_RIFF.size).toLong() and UNSIGNED_INT_MASK
            val payloadStart = position + CHUNK_HEADER_SIZE
            val payloadSize = minOf(declaredSize, (bytes.size - payloadStart).toLong())
            when (chunkId) {
                CHUNK_ID_FMT -> format = parseFormat(buffer, payloadStart, payloadSize)
                CHUNK_ID_DATA -> {
                    dataOffset = payloadStart
                    dataSize = payloadSize.toInt()
                }
            }
            val paddedSize = payloadSize + payloadSize % 2
            position = payloadStart + paddedSize.toInt()
        }

        val parsedFormat = format ?: throw WavFormatException("missing fmt chunk")
        if (dataOffset < 0) throw WavFormatException("missing data chunk")
        if (parsedFormat.channelCount != AudioSpec.CHANNEL_COUNT) {
            throw WavFormatException("only mono WAV is supported, found ${parsedFormat.channelCount} channels")
        }
        if (parsedFormat.bitsPerSample != AudioSpec.BITS_PER_SAMPLE) {
            throw WavFormatException("only 16-bit WAV is supported, found ${parsedFormat.bitsPerSample} bits")
        }
        val sampleCount = dataSize / AudioSpec.BYTES_PER_SAMPLE
        val samples = ShortArray(sampleCount)
        for (index in 0 until sampleCount) {
            samples[index] = buffer.getShort(dataOffset + index * AudioSpec.BYTES_PER_SAMPLE)
        }
        return PcmAudio(samples, parsedFormat.sampleRateHz)
    }

    private fun parseFormat(buffer: ByteBuffer, payloadStart: Int, payloadSize: Long): WavFormat {
        if (payloadSize < FMT_CHUNK_SIZE) throw WavFormatException("truncated fmt chunk")
        val audioFormat = buffer.getShort(payloadStart).toInt()
        if (audioFormat != WavFormat.FORMAT_PCM) {
            throw WavFormatException("unsupported WAV format $audioFormat, only PCM is supported")
        }
        return WavFormat(
            sampleRateHz = buffer.getInt(payloadStart + 4),
            channelCount = buffer.getShort(payloadStart + 2).toInt(),
            bitsPerSample = buffer.getShort(payloadStart + 14).toInt(),
        )
    }

    private fun ByteBuffer.matches(offset: Int, fourCc: ByteArray): Boolean {
        if (offset + fourCc.size > limit()) return false
        for (index in fourCc.indices) {
            if (get(offset + index) != fourCc[index]) return false
        }
        return true
    }

    private companion object {
        val FOURCC_RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
        val FOURCC_WAVE = "WAVE".toByteArray(Charsets.US_ASCII)
        val FOURCC_FMT = "fmt ".toByteArray(Charsets.US_ASCII)
        val FOURCC_DATA = "data".toByteArray(Charsets.US_ASCII)

        const val CHUNK_ID_FMT = "fmt "
        const val CHUNK_ID_DATA = "data"
        const val FOURCC_OFFSET = 0
        const val WAVE_OFFSET = 8
        const val CHUNK_HEADER_SIZE = 8
        const val FMT_CHUNK_SIZE = 16
        const val RIFF_HEADER_SIZE = 12
        const val MINIMUM_SIZE = RIFF_HEADER_SIZE
        const val RIFF_SIZE_BASE = 36
        const val HEADER_SIZE = 44
        const val UNSIGNED_INT_MASK = 0xFFFFFFFFL
    }
}
