package com.learnhuayu.core.audio.capture

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.audio.vad.VadEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRecordRecorderTest {

    @Test
    fun `starts idle and reports recording after start`() = runTest {
        val device = FakePcmCaptureDevice()
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            assertThat(recorder.state.value).isEqualTo(RecorderState.Idle)

            recorder.start()

            assertThat(recorder.state.value).isEqualTo(RecorderState.Recording)
            assertThat(device.startCount).isEqualTo(1)
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `captures frames into a mono recording and reports the level`() = runTest {
        val device = FakePcmCaptureDevice(sampleRateHz = 1_000, bufferSizeInShorts = 4)
        device.enqueue(shortArrayOf(16_384, 16_384, 16_384, 16_384))
        device.enqueue(shortArrayOf(-16_384, -16_384, -16_384, -16_384))
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            recorder.start()

            assertThat(recorder.level.value).isWithin(0.001f).of(0.5f)

            val audio = recorder.stop()

            assertThat(audio.sampleRateHz).isEqualTo(1_000)
            assertArrayEquals(
                shortArrayOf(16_384, 16_384, 16_384, 16_384, -16_384, -16_384, -16_384, -16_384),
                audio.samples,
            )
            assertThat(recorder.level.value).isEqualTo(0f)
            assertThat(recorder.state.value).isEqualTo(RecorderState.Idle)
            assertThat(device.stopped).isTrue()
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `level flow emits the captured amplitude and returns to zero on stop`() = runTest {
        val device = FakePcmCaptureDevice(sampleRateHz = 1_000, bufferSizeInShorts = 4)
        device.enqueue(ShortArray(4) { Short.MAX_VALUE })
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            recorder.level.test {
                assertThat(awaitItem()).isEqualTo(0f)

                recorder.start()
                assertThat(awaitItem()).isWithin(0.001f).of(1f)

                recorder.stop()
                assertThat(awaitItem()).isEqualTo(0f)
            }
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `read errors surface as a failed state without losing captured samples`() = runTest {
        val device = FakePcmCaptureDevice()
        device.enqueue(shortArrayOf(1_000, 1_000, 1_000, 1_000))
        device.readError = -3
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            recorder.start()

            assertThat(recorder.state.value).isEqualTo(RecorderState.Failed("AudioRecord read failed (-3)"))

            val audio = recorder.stop()

            assertThat(audio.sampleCount).isEqualTo(4)
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `device start failures surface as a failed state`() = runTest {
        val device = FakePcmCaptureDevice()
        device.startError = "microphone unavailable"
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            recorder.start()

            assertThat(recorder.state.value).isEqualTo(RecorderState.Failed("microphone unavailable"))
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `starting twice while recording does not start the device again`() = runTest {
        val device = FakePcmCaptureDevice()
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            recorder.start()
            recorder.start()

            assertThat(device.startCount).isEqualTo(1)
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `stop without start returns empty audio`() = runTest {
        val device = FakePcmCaptureDevice(sampleRateHz = 1_000)
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            val audio = recorder.stop()

            assertThat(audio.sampleCount).isEqualTo(0)
            assertThat(audio.sampleRateHz).isEqualTo(1_000)
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `release stops and releases the device and resets the state`() = runTest {
        val device = FakePcmCaptureDevice()
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        recorder.start()

        recorder.release()

        assertThat(device.stopped).isTrue()
        assertThat(device.released).isTrue()
        assertThat(recorder.state.value).isEqualTo(RecorderState.Idle)
        assertThat(recorder.level.value).isEqualTo(0f)
    }

    @Test
    fun `emits the detector's SpeechEnded event while capturing`() = runTest {
        val device = FakePcmCaptureDevice(sampleRateHz = 1_000, bufferSizeInShorts = 4)
        device.enqueue(ShortArray(4))
        val detector = FakeEndOfSpeechDetector(framesPerUtterance = 1, speechEndedAtMs = 120L)
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler), detector)
        try {
            recorder.vadEvents.test {
                recorder.start()

                assertThat(awaitItem()).isEqualTo(VadEvent.SpeechEnded(120L))
                assertThat(detector.acceptedFrames).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `resets the detector after SpeechEnded so the next utterance is tracked independently`() = runTest {
        val device = FakePcmCaptureDevice(sampleRateHz = 1_000, bufferSizeInShorts = 4)
        repeat(4) { device.enqueue(ShortArray(4)) }
        val detector = FakeEndOfSpeechDetector(framesPerUtterance = 2, speechEndedAtMs = 300L)
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler), detector)
        try {
            recorder.vadEvents.test {
                recorder.start()

                assertThat(awaitItem()).isEqualTo(VadEvent.SpeechEnded(300L))
                assertThat(awaitItem()).isEqualTo(VadEvent.SpeechEnded(300L))
                assertThat(detector.resetCount).isEqualTo(3)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `resets the detector each time a recording starts`() = runTest {
        val device = FakePcmCaptureDevice()
        val detector = FakeEndOfSpeechDetector()
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler), detector)
        try {
            recorder.start()

            assertThat(detector.resetCount).isEqualTo(1)
        } finally {
            recorder.release()
        }
    }

    @Test
    fun `a recorder without a detector never signals vad events`() = runTest {
        val device = FakePcmCaptureDevice(sampleRateHz = 1_000, bufferSizeInShorts = 4)
        device.enqueue(ShortArray(4))
        val recorder = AudioRecordRecorder(device, UnconfinedTestDispatcher(testScheduler))
        try {
            recorder.vadEvents.test {
                recorder.start()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            recorder.release()
        }
    }
}
