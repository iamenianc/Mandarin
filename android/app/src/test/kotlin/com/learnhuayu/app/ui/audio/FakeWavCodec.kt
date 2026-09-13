package com.learnhuayu.app.ui.audio

import com.learnhuayu.core.audio.pcm.AudioSpec
import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.wav.WavCodec
import java.io.File

class FakeWavCodec : WavCodec {

    var writtenFile: File? = null
        private set

    var writtenAudio: PcmAudio? = null
        private set

    override fun encode(audio: PcmAudio): ByteArray = ByteArray(audio.samples.size * AudioSpec.BYTES_PER_SAMPLE)

    override fun decode(bytes: ByteArray): PcmAudio = throw UnsupportedOperationException("decode is not used by the audio check screen")

    override fun write(file: File, audio: PcmAudio) {
        writtenFile = file
        writtenAudio = audio
    }
}
