package com.learnhuayu.core.audio

import com.learnhuayu.core.audio.capture.AudioRecordCaptureDevice
import com.learnhuayu.core.audio.capture.AudioRecordRecorder
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.PcmCaptureDevice
import com.learnhuayu.core.audio.pcm.AudioSpec
import com.learnhuayu.core.audio.playback.AudioPlayer
import com.learnhuayu.core.audio.playback.ExoAudioPlayer
import com.learnhuayu.core.audio.vad.EndOfSpeechDetector
import com.learnhuayu.core.audio.vad.EnergyZeroCrossingEndOfSpeechDetector
import com.learnhuayu.core.audio.wav.Pcm16WavCodec
import com.learnhuayu.core.audio.wav.WavCodec
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreAudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(implementation: ExoAudioPlayer): AudioPlayer

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(implementation: AudioRecordRecorder): AudioRecorder

    companion object {

        @Provides
        @Singleton
        fun providePcmCaptureDevice(): PcmCaptureDevice = AudioRecordCaptureDevice(sampleRateHz = AudioSpec.SAMPLE_RATE_HZ)

        @Provides
        @Singleton
        fun provideEndOfSpeechDetector(): EndOfSpeechDetector = EnergyZeroCrossingEndOfSpeechDetector(sampleRateHz = AudioSpec.SAMPLE_RATE_HZ)

        @Provides
        @Singleton
        fun provideWavCodec(): WavCodec = Pcm16WavCodec()
    }
}
