package com.learnhuayu.core.audio.wav

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.audio.pcm.PcmAudio
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class Pcm16WavCodecTest {

    private val codec = Pcm16WavCodec()

    @Test
    fun `encodes a canonical 24 kHz mono 16-bit header`() {
        val encoded = codec.encode(PcmAudio(shortArrayOf(0), sampleRateHz = 24_000))

        assertThat(encoded.size).isEqualTo(46)
        assertThat(fourCc(encoded, 0)).isEqualTo("RIFF")
        assertThat(littleEndianInt(encoded, 4)).isEqualTo(38)
        assertThat(fourCc(encoded, 8)).isEqualTo("WAVE")
        assertThat(fourCc(encoded, 12)).isEqualTo("fmt ")
        assertThat(littleEndianInt(encoded, 16)).isEqualTo(16)
        assertThat(littleEndianShort(encoded, 20)).isEqualTo(1)
        assertThat(littleEndianShort(encoded, 22)).isEqualTo(1)
        assertThat(littleEndianInt(encoded, 24)).isEqualTo(24_000)
        assertThat(littleEndianInt(encoded, 28)).isEqualTo(48_000)
        assertThat(littleEndianShort(encoded, 32)).isEqualTo(2)
        assertThat(littleEndianShort(encoded, 34)).isEqualTo(16)
        assertThat(fourCc(encoded, 36)).isEqualTo("data")
        assertThat(littleEndianInt(encoded, 40)).isEqualTo(2)
        assertThat(littleEndianShort(encoded, 44)).isEqualTo(0)
    }

    @Test
    fun `round trips signed sample values`() {
        val samples = shortArrayOf(0, 1, -1, Short.MAX_VALUE, Short.MIN_VALUE, 12_345, -12_345)
        val audio = PcmAudio(samples, sampleRateHz = 24_000)

        val decoded = codec.decode(codec.encode(audio))

        assertThat(decoded.sampleRateHz).isEqualTo(24_000)
        assertArrayEquals(samples, decoded.samples)
    }

    @Test
    fun `round trips a sine tone`() {
        val samples = ShortArray(480) { index ->
            (sin(2.0 * Math.PI * 440.0 * index / 24_000.0) * 20_000).toInt().toShort()
        }

        val decoded = codec.decode(codec.encode(PcmAudio(samples, sampleRateHz = 24_000)))

        assertArrayEquals(samples, decoded.samples)
    }

    @Test
    fun `round trips empty audio`() {
        val decoded = codec.decode(codec.encode(PcmAudio(ShortArray(0), sampleRateHz = 24_000)))

        assertThat(decoded.sampleCount).isEqualTo(0)
        assertThat(decoded.sampleRateHz).isEqualTo(24_000)
    }

    @Test
    fun `writes and reads a wav file`() {
        val file = File.createTempFile("core-audio", ".wav")
        try {
            val audio = PcmAudio(shortArrayOf(100, -100, 32_767), sampleRateHz = 24_000)

            codec.write(file, audio)
            val read = codec.read(file)

            assertArrayEquals(audio.samples, read.samples)
            assertThat(read.sampleRateHz).isEqualTo(24_000)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `decodes a file with an extra chunk before fmt`() {
        val samples = shortArrayOf(7, -7)
        val payload = buildWav(
            samples = samples,
            extraChunks = listOf("JUNK" to ByteArray(3) { 0x42 }),
        )

        val decoded = codec.decode(payload)

        assertArrayEquals(samples, decoded.samples)
    }

    @Test
    fun `decodes a data chunk with an unknown declared size`() {
        val samples = shortArrayOf(7, -7, 9)
        val payload = buildWav(samples = samples, declaredDataSize = -1)

        val decoded = codec.decode(payload)

        assertArrayEquals(samples, decoded.samples)
    }

    @Test
    fun `rejects a stereo wav`() {
        val payload = buildWav(samples = shortArrayOf(1, 2, 3, 4), channelCount = 2)

        assertThrows(WavFormatException::class.java) { codec.decode(payload) }
    }

    @Test
    fun `rejects an eight bit wav`() {
        val payload = buildWav(samples = shortArrayOf(1, 2), bitsPerSample = 8)

        assertThrows(WavFormatException::class.java) { codec.decode(payload) }
    }

    @Test
    fun `rejects a non pcm format`() {
        val payload = buildWav(samples = shortArrayOf(1, 2), audioFormat = 3)

        assertThrows(WavFormatException::class.java) { codec.decode(payload) }
    }

    @Test
    fun `rejects a payload that is not riff wave`() {
        assertThrows(WavFormatException::class.java) { codec.decode(ByteArray(64)) }
        assertThrows(WavFormatException::class.java) { codec.decode(ByteArray(4)) }
    }

    private fun buildWav(
        samples: ShortArray,
        sampleRateHz: Int = 24_000,
        channelCount: Int = 1,
        bitsPerSample: Int = 16,
        audioFormat: Int = 1,
        declaredDataSize: Int = samples.size * 2,
        extraChunks: List<Pair<String, ByteArray>> = emptyList(),
    ): ByteArray {
        val fmt = ByteArrayOutputStream()
        fmt.write(littleEndianShort(audioFormat))
        fmt.write(littleEndianShort(channelCount))
        fmt.write(littleEndianInt(sampleRateHz))
        val blockAlign = channelCount * (bitsPerSample / 8)
        fmt.write(littleEndianInt(sampleRateHz * blockAlign))
        fmt.write(littleEndianShort(blockAlign))
        fmt.write(littleEndianShort(bitsPerSample))

        val data = ByteArrayOutputStream()
        for (sample in samples) {
            data.write(littleEndianShort(sample.toInt()))
        }

        val body = ByteArrayOutputStream()
        for ((id, payload) in extraChunks) {
            body.write(id.toByteArray(Charsets.US_ASCII))
            body.write(littleEndianInt(payload.size))
            body.write(payload)
            if (payload.size % 2 == 1) body.write(0)
        }
        body.write("fmt ".toByteArray(Charsets.US_ASCII))
        body.write(littleEndianInt(fmt.size()))
        body.write(fmt.toByteArray())
        body.write("data".toByteArray(Charsets.US_ASCII))
        body.write(littleEndianInt(declaredDataSize))
        body.write(data.toByteArray())

        val bodyBytes = body.toByteArray()
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        out.write(littleEndianInt(bodyBytes.size))
        out.write("WAVE".toByteArray(Charsets.US_ASCII))
        out.write(bodyBytes)
        return out.toByteArray()
    }

    private fun fourCc(bytes: ByteArray, offset: Int): String =
        String(bytes, offset, 4, Charsets.US_ASCII)

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun littleEndianShort(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

    private fun littleEndianShort(value: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()

    private fun littleEndianInt(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
