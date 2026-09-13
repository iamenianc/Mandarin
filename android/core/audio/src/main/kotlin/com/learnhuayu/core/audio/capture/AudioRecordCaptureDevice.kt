package com.learnhuayu.core.audio.capture

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.learnhuayu.core.audio.pcm.AudioSpec

class AudioRecordCaptureDevice(
    override val sampleRateHz: Int = AudioSpec.SAMPLE_RATE_HZ,
    private val audioSource: Int = MediaRecorder.AudioSource.MIC,
) : PcmCaptureDevice {

    override val bufferSizeInShorts: Int = calculateBufferSizeInShorts(sampleRateHz)

    private var audioRecord: AudioRecord? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        val record = audioRecord ?: createAudioRecord().also { audioRecord = it }
        check(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord failed to initialize" }
        record.startRecording()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = audioRecord?.read(buffer, offset, size) ?: READ_ERROR

    override fun stop() {
        val record = audioRecord ?: return
        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            record.stop()
        }
    }

    override fun release() {
        audioRecord?.release()
        audioRecord = null
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(): AudioRecord = AudioRecord.Builder()
        .setAudioSource(audioSource)
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRateHz)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build(),
        )
        .setBufferSizeInBytes(bufferSizeInShorts * AudioSpec.BYTES_PER_SAMPLE)
        .build()

    private companion object {
        const val READ_ERROR = -1

        fun calculateBufferSizeInShorts(sampleRateHz: Int): Int {
            val minimumBytes = AudioRecord.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            check(minimumBytes > 0) { "no AudioRecord buffer for $sampleRateHz Hz" }
            return maxOf(minimumBytes / AudioSpec.BYTES_PER_SAMPLE, sampleRateHz / 10)
        }
    }
}
