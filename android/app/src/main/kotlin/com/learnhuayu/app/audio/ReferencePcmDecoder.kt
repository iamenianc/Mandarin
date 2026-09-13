package com.learnhuayu.app.audio

import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import com.learnhuayu.core.ai.AudioClip
import com.learnhuayu.core.ai.AudioFormat
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.wav.WavCodec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decodes one bundled reference clip to raw PCM so the on-device tone pipeline can measure it
 * (ADR 0014). The bundled clips are OGG (Kokoro output, ADR 0006) but the same path accepts
 * MP3 and M4A; WAV is decoded directly because audio capture already uses the WAV helper.
 * Decoding happens off the main thread in the caller. A format the device cannot decode
 * returns `null`, which the evidence source treats as a missing reference.
 */
interface ReferencePcmDecoder {
    suspend fun decode(clip: AudioClip): PcmAudio?
}

@Singleton
class AndroidReferencePcmDecoder @Inject constructor(
    private val wavCodec: WavCodec,
) : ReferencePcmDecoder {

    override suspend fun decode(clip: AudioClip): PcmAudio? = when (clip.format) {
        AudioFormat.WAV -> decodeWav(clip.bytes)
        AudioFormat.OGG, AudioFormat.MP3, AudioFormat.M4A -> decodeCompressed(clip.bytes)
        AudioFormat.WEBM, AudioFormat.FLAC -> null
    }

    private fun decodeWav(bytes: ByteArray): PcmAudio? = try {
        wavCodec.decode(bytes)
    } catch (error: Exception) {
        null
    }

    private fun decodeCompressed(bytes: ByteArray): PcmAudio? {
        if (bytes.isEmpty()) return null
        val dataSource = ByteArrayMediaDataSource(bytes)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(dataSource)
            val trackIndex = audioTrack(extractor) ?: return null
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
            if (!mime.startsWith("audio/")) return null
            val sampleRateHz = inputFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: return null
            var channelCount = inputFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val samples = ShortBuffer()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return null
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        channelCount = codec.outputFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: channelCount
                    }

                    outputIndex >= 0 -> {
                        if (bufferInfo.size > 0) {
                            codec.getOutputBuffer(outputIndex)?.let { outputBuffer ->
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                samples.append(outputBuffer, channelCount)
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }

            if (samples.isEmpty) return null
            return PcmAudio(samples.toShortArray(), sampleRateHz)
        } catch (error: Exception) {
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
            runCatching { dataSource.close() }
        }
    }

    private fun audioTrack(extractor: MediaExtractor): Int? {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return index
        }
        return null
    }

    private fun MediaFormat.intOrNull(key: String): Int? = if (containsKey(key)) getInteger(key) else null

    private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position < 0L || position >= data.size) return -1
            val count = minOf(size.toLong(), data.size - position).toInt()
            if (count <= 0) return -1
            System.arraycopy(data, position.toInt(), buffer, offset, count)
            return count
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() = Unit
    }

    private class ShortBuffer {
        private var data = ShortArray(INITIAL_CAPACITY)
        private var size = 0

        val isEmpty: Boolean get() = size == 0

        fun append(buffer: ByteBuffer, channelCount: Int) {
            val channels = channelCount.coerceAtLeast(1)
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val shorts = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val frames = shorts.remaining() / channels
            ensure(size + frames)
            for (frame in 0 until frames) {
                var sum = 0
                for (channel in 0 until channels) sum += shorts.get().toInt()
                data[size++] = (sum / channels).toShort()
            }
        }

        fun toShortArray(): ShortArray = data.copyOf(size)

        private fun ensure(capacity: Int) {
            if (capacity <= data.size) return
            var newCapacity = data.size * 2
            while (newCapacity < capacity) newCapacity *= 2
            data = data.copyOf(newCapacity)
        }

        private companion object {
            const val INITIAL_CAPACITY = 16_384
        }
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
