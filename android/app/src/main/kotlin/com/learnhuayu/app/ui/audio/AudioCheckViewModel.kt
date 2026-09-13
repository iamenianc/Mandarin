package com.learnhuayu.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.app.audio.RecordingStore
import com.learnhuayu.core.audio.capture.AudioRecorder
import com.learnhuayu.core.audio.capture.RecorderState
import com.learnhuayu.core.audio.playback.AudioPlayer
import com.learnhuayu.core.audio.playback.AudioSource
import com.learnhuayu.core.audio.playback.PlaybackState
import com.learnhuayu.core.audio.playback.PlaybackStatus
import com.learnhuayu.core.audio.wav.WavCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

const val REFERENCE_CLIP_ASSET = "audio/reference/tones/tone1-contour.wav"

enum class MicrophonePermission {
    NotRequested,
    Requesting,
    Granted,
    Denied,
}

data class AudioPlaybackSlot(
    val state: PlaybackState = PlaybackState(),
    val hasPlayed: Boolean = false,
)

data class AudioCheckUiState(
    val permission: MicrophonePermission = MicrophonePermission.NotRequested,
    val recorderState: RecorderState = RecorderState.Idle,
    val referencePlayback: AudioPlaybackSlot = AudioPlaybackSlot(),
    val recordingPlayback: AudioPlaybackSlot = AudioPlaybackSlot(),
    val level: Float = 0f,
    val hasRecording: Boolean = false,
    val processing: Boolean = false,
    val errorMessage: String? = null,
) {
    val isRecording: Boolean
        get() = recorderState is RecorderState.Recording

    val hasRecordingPlayback: Boolean
        get() = hasRecording && !isRecording && !processing
}

@HiltViewModel
class AudioCheckViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val audioRecorder: AudioRecorder,
    private val wavCodec: WavCodec,
    private val recordingStore: RecordingStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioCheckUiState())
    val uiState: StateFlow<AudioCheckUiState> = _uiState.asStateFlow()

    private val referenceSource = AudioSource.Asset(REFERENCE_CLIP_ASSET)

    private var recordingFile: File? = null
    private var stopJob: Job? = null

    init {
        viewModelScope.launch {
            audioRecorder.state.collect { state -> onRecorderState(state) }
        }
        viewModelScope.launch {
            audioRecorder.level.collect { level -> _uiState.update { it.copy(level = level) } }
        }
        viewModelScope.launch {
            audioPlayer.state.collect { state -> onPlaybackState(state) }
        }
    }

    fun onPermissionStatusChecked(granted: Boolean) {
        _uiState.update { state ->
            when {
                granted -> state.copy(permission = MicrophonePermission.Granted)
                state.permission == MicrophonePermission.Granted ->
                    state.copy(permission = MicrophonePermission.NotRequested)

                else -> state
            }
        }
    }

    fun onRequestPermission() {
        if (_uiState.value.permission == MicrophonePermission.Granted) return
        _uiState.update { it.copy(permission = MicrophonePermission.Requesting) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permission = if (granted) {
                    MicrophonePermission.Granted
                } else {
                    MicrophonePermission.Denied
                },
            )
        }
    }

    fun onRecordClick() {
        val state = _uiState.value
        when {
            state.processing -> Unit
            state.isRecording -> stopRecording()
            state.permission != MicrophonePermission.Granted -> onRequestPermission()
            else -> startRecording()
        }
    }

    fun onPlayReferenceClick() {
        val state = _uiState.value
        if (state.processing || state.isRecording) return
        _uiState.update { it.copy(errorMessage = null) }
        audioPlayer.play(referenceSource)
    }

    fun onPlayRecordingClick() {
        val state = _uiState.value
        val source = recordingFile?.let { AudioSource.LocalFile(it.path) } ?: return
        if (state.processing || state.isRecording) return
        _uiState.update { it.copy(errorMessage = null) }
        audioPlayer.play(source)
    }

    fun onLeaveScreen() {
        if (!_uiState.value.isRecording) return
        viewModelScope.launch {
            runCatching { audioRecorder.stop() }
        }
    }

    private fun startRecording() {
        audioPlayer.pause()
        _uiState.update { it.copy(errorMessage = null) }
        audioRecorder.start()
    }

    private fun stopRecording() {
        if (stopJob?.isActive == true) return
        _uiState.update { it.copy(processing = true) }
        stopJob = viewModelScope.launch {
            try {
                val audio = audioRecorder.stop()
                val file = recordingStore.newRecordingFile()
                wavCodec.write(file, audio)
                recordingFile = file
                _uiState.update { it.copy(hasRecording = true) }
                audioPlayer.play(AudioSource.LocalFile(file.path))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: error::class.java.simpleName) }
            } finally {
                _uiState.update { it.copy(processing = false) }
            }
        }
    }

    private fun onRecorderState(state: RecorderState) {
        _uiState.update {
            it.copy(
                recorderState = state,
                errorMessage = (state as? RecorderState.Failed)?.message ?: it.errorMessage,
            )
        }
    }

    private fun onPlaybackState(state: PlaybackState) {
        val source = state.source
        when {
            source == referenceSource -> routePlayback(reference = state, recording = PlaybackState())
            isRecordingSource(source) -> routePlayback(reference = PlaybackState(), recording = state)
            else -> routePlayback(reference = PlaybackState(), recording = PlaybackState())
        }
        (state.status as? PlaybackStatus.Failed)?.let { failure ->
            _uiState.update { it.copy(errorMessage = failure.message) }
        }
    }

    private fun routePlayback(reference: PlaybackState, recording: PlaybackState) {
        _uiState.update { state ->
            state.copy(
                referencePlayback = state.referencePlayback.with(reference),
                recordingPlayback = state.recordingPlayback.with(recording),
            )
        }
    }

    private fun AudioPlaybackSlot.with(playback: PlaybackState): AudioPlaybackSlot = AudioPlaybackSlot(state = playback, hasPlayed = hasPlayed || playback.source != null)

    private fun isRecordingSource(source: AudioSource?): Boolean {
        val path = recordingFile?.path ?: return false
        return source is AudioSource.LocalFile && source.path == path
    }
}
