package com.learnhuayu.app.ui.audio

import app.cash.turbine.test
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.audio.playback.PlaybackState
import com.learnhuayu.core.audio.playback.PlaybackStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class AudioCheckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recordingFile = File("build/tmp/audio-check-test/recording.wav")
    private val player = FakeAudioPlayer()
    private val recorder = FakeAudioRecorder()
    private val wavCodec = FakeWavCodec()
    private val recordingStore = FakeRecordingStore(recordingFile)

    private lateinit var viewModel: AudioCheckViewModel

    @Before
    fun setUp() {
        viewModel = AudioCheckViewModel(player, recorder, wavCodec, recordingStore)
    }

    @Test
    fun `play reference sends the bundled tone asset to the player`() = runTest {
        viewModel.onPlayReferenceClick()

        assertEquals(listOf(AudioSource.Asset(REFERENCE_CLIP_ASSET)), player.playedSources)
        assertTrue(viewModel.uiState.value.referencePlayback.hasPlayed)
        assertTrue(viewModel.uiState.value.referencePlayback.state.isPlaying)
    }

    @Test
    fun `record click before the first request asks for permission and keeps the recorder off`() = runTest {
        viewModel.onRecordClick()

        assertEquals(MicrophonePermission.Requesting, viewModel.uiState.value.permission)
        assertEquals(0, recorder.startCount)
        assertTrue(player.playedSources.isEmpty())
    }

    @Test
    fun `denied permission never starts the recorder`() = runTest {
        viewModel.onPermissionResult(false)

        viewModel.onRecordClick()

        assertEquals(0, recorder.startCount)
        assertEquals(MicrophonePermission.Requesting, viewModel.uiState.value.permission)

        viewModel.onPermissionResult(false)

        assertEquals(MicrophonePermission.Denied, viewModel.uiState.value.permission)
        assertEquals(0, recorder.startCount)
    }

    @Test
    fun `granted permission records, shares the live level, and stops`() = runTest {
        viewModel.onPermissionStatusChecked(true)

        viewModel.onRecordClick()

        assertEquals(1, recorder.startCount)
        assertTrue(viewModel.uiState.value.isRecording)

        recorder.setLevel(0.42f)

        assertEquals(0.42f, viewModel.uiState.value.level, 0.0001f)

        viewModel.onRecordClick()

        assertEquals(1, recorder.stopCount)
        assertFalse(viewModel.uiState.value.isRecording)
        assertTrue(viewModel.uiState.value.hasRecording)
    }

    @Test
    fun `live level is published while recording`() = runTest {
        viewModel.onPermissionStatusChecked(true)
        viewModel.onRecordClick()

        viewModel.uiState.test {
            assertEquals(0f, awaitItem().level, 0.0001f)

            recorder.setLevel(0.75f)

            assertEquals(0.75f, awaitItem().level, 0.0001f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stopping a recording writes a wav and plays the local file back`() = runTest {
        viewModel.onPermissionStatusChecked(true)
        viewModel.onRecordClick()

        viewModel.onRecordClick()

        assertEquals(recordingFile, wavCodec.writtenFile)
        assertEquals(recorder.stopResult, wavCodec.writtenAudio)
        assertEquals(listOf(AudioSource.LocalFile(recordingFile.path)), player.playedSources)
        assertTrue(viewModel.uiState.value.recordingPlayback.hasPlayed)
    }

    @Test
    fun `stop reports processing until the wav write completes`() = runTest {
        viewModel.onPermissionStatusChecked(true)
        viewModel.onRecordClick()
        recorder.stopGate = CompletableDeferred()

        viewModel.onRecordClick()

        assertTrue(viewModel.uiState.value.processing)
        assertFalse(viewModel.uiState.value.hasRecording)

        recorder.stopGate?.complete(Unit)

        assertFalse(viewModel.uiState.value.processing)
        assertTrue(viewModel.uiState.value.hasRecording)
    }

    @Test
    fun `starting a recording pauses any playing playback`() = runTest {
        viewModel.onPlayReferenceClick()
        viewModel.onPermissionStatusChecked(true)

        viewModel.onRecordClick()

        assertEquals(1, player.pauseCount)
        assertEquals(1, recorder.startCount)
    }

    @Test
    fun `playback is ignored while recording`() = runTest {
        viewModel.onPermissionStatusChecked(true)
        viewModel.onRecordClick()

        viewModel.onPlayReferenceClick()
        viewModel.onPlayRecordingClick()

        assertTrue(player.playedSources.isEmpty())
    }

    @Test
    fun `leaving the screen stops an active recording`() = runTest {
        viewModel.onPermissionStatusChecked(true)
        viewModel.onRecordClick()

        viewModel.onLeaveScreen()

        assertEquals(1, recorder.stopCount)
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun `recorder failure surfaces its message and state`() = runTest {
        recorder.fail("microphone unavailable")

        assertEquals(RecorderState.Failed("microphone unavailable"), viewModel.uiState.value.recorderState)
        assertEquals("microphone unavailable", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `playback failure surfaces its message and state`() = runTest {
        player.emit(
            PlaybackState(
                source = AudioSource.Asset(REFERENCE_CLIP_ASSET),
                status = PlaybackStatus.Failed("decode error"),
            ),
        )

        assertEquals("decode error", viewModel.uiState.value.errorMessage)
        assertEquals(
            PlaybackStatus.Failed("decode error"),
            viewModel.uiState.value.referencePlayback.state.status,
        )
    }

    @Test
    fun `permission status check grants and revocation downgrades`() = runTest {
        viewModel.onPermissionStatusChecked(true)

        assertEquals(MicrophonePermission.Granted, viewModel.uiState.value.permission)

        viewModel.onPermissionStatusChecked(false)

        assertEquals(MicrophonePermission.NotRequested, viewModel.uiState.value.permission)
    }
}
