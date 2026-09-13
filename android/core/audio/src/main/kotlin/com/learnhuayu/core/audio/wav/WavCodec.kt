package com.learnhuayu.core.audio.wav

import com.learnhuayu.core.audio.pcm.PcmAudio
import java.io.File

interface WavCodec {

    fun encode(audio: PcmAudio): ByteArray

    fun decode(bytes: ByteArray): PcmAudio

    fun write(file: File, audio: PcmAudio) {
        file.parentFile?.mkdirs()
        file.writeBytes(encode(audio))
    }

    fun read(file: File): PcmAudio = decode(file.readBytes())
}
