package com.learnhuayu.core.audio.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface PlaybackCommand {

    data class Play(val source: AudioSource) : PlaybackCommand

    data object Pause : PlaybackCommand

    data object Replay : PlaybackCommand

    data class Seek(val positionMs: Long) : PlaybackCommand

    data object Release : PlaybackCommand
}

internal sealed interface PlaybackEvent {

    data class Prepared(val durationMs: Long?) : PlaybackEvent

    data object StartedPlaying : PlaybackEvent

    data object StoppedPlaying : PlaybackEvent

    data object Ended : PlaybackEvent

    data class PositionChanged(val positionMs: Long) : PlaybackEvent

    data class Failed(val message: String) : PlaybackEvent
}

internal sealed interface PlaybackEffect {

    data class Load(val source: AudioSource) : PlaybackEffect

    data object StartPlayback : PlaybackEffect

    data object PausePlayback : PlaybackEffect

    data class SeekTo(val positionMs: Long) : PlaybackEffect

    data object ReleasePlayer : PlaybackEffect
}

internal class PlaybackStateMachine(initialState: PlaybackState = PlaybackState()) {

    private val mutableState = MutableStateFlow(initialState)

    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    fun onCommand(command: PlaybackCommand): List<PlaybackEffect> {
        val current = mutableState.value
        return when (command) {
            is PlaybackCommand.Play -> play(command.source, current)
            PlaybackCommand.Pause -> pause(current)
            PlaybackCommand.Replay -> replay(current)
            is PlaybackCommand.Seek -> seek(command.positionMs, current)
            PlaybackCommand.Release -> release()
        }
    }

    fun onEvent(event: PlaybackEvent) {
        val current = mutableState.value
        mutableState.value = when (event) {
            is PlaybackEvent.Prepared -> prepared(event, current)
            PlaybackEvent.StartedPlaying -> startedPlaying(current)
            PlaybackEvent.StoppedPlaying -> stoppedPlaying(current)
            PlaybackEvent.Ended -> ended(current)
            is PlaybackEvent.PositionChanged -> positionChanged(event, current)
            is PlaybackEvent.Failed -> current.copy(status = PlaybackStatus.Failed(event.message))
        }
    }

    private fun play(source: AudioSource, current: PlaybackState): List<PlaybackEffect> {
        if (current.source != source) return load(source)
        return when (current.status) {
            PlaybackStatus.Playing, PlaybackStatus.Buffering -> emptyList()
            PlaybackStatus.Paused -> {
                update(current.copy(status = PlaybackStatus.Buffering))
                listOf(PlaybackEffect.StartPlayback)
            }
            PlaybackStatus.Ended -> {
                update(current.copy(status = PlaybackStatus.Buffering, positionMs = 0L))
                listOf(PlaybackEffect.SeekTo(0L), PlaybackEffect.StartPlayback)
            }
            PlaybackStatus.Idle, is PlaybackStatus.Failed -> load(source)
        }
    }

    private fun load(source: AudioSource): List<PlaybackEffect> {
        update(PlaybackState(source = source, status = PlaybackStatus.Buffering))
        return listOf(PlaybackEffect.Load(source), PlaybackEffect.StartPlayback)
    }

    private fun pause(current: PlaybackState): List<PlaybackEffect> {
        if (current.status != PlaybackStatus.Playing && current.status != PlaybackStatus.Buffering) {
            return emptyList()
        }
        update(current.copy(status = PlaybackStatus.Paused))
        return listOf(PlaybackEffect.PausePlayback)
    }

    private fun replay(current: PlaybackState): List<PlaybackEffect> {
        val source = current.source ?: return emptyList()
        if (current.status == PlaybackStatus.Idle || current.status is PlaybackStatus.Failed) {
            return load(source)
        }
        update(current.copy(status = PlaybackStatus.Buffering, positionMs = 0L))
        return listOf(PlaybackEffect.SeekTo(0L), PlaybackEffect.StartPlayback)
    }

    private fun seek(positionMs: Long, current: PlaybackState): List<PlaybackEffect> {
        if (current.source == null || current.status == PlaybackStatus.Idle) return emptyList()
        val durationMs = current.durationMs
        val target = positionMs.coerceAtLeast(0L).let { if (durationMs == null) it else it.coerceAtMost(durationMs) }
        val status = if (current.status == PlaybackStatus.Ended) PlaybackStatus.Paused else current.status
        update(current.copy(status = status, positionMs = target))
        return listOf(PlaybackEffect.SeekTo(target))
    }

    private fun release(): List<PlaybackEffect> {
        update(PlaybackState())
        return listOf(PlaybackEffect.ReleasePlayer)
    }

    private fun prepared(event: PlaybackEvent.Prepared, current: PlaybackState): PlaybackState {
        if (current.status == PlaybackStatus.Idle || current.status is PlaybackStatus.Failed) return current
        return current.copy(durationMs = event.durationMs ?: current.durationMs)
    }

    private fun startedPlaying(current: PlaybackState): PlaybackState {
        if (current.status == PlaybackStatus.Idle) return current
        return current.copy(status = PlaybackStatus.Playing)
    }

    private fun stoppedPlaying(current: PlaybackState): PlaybackState {
        if (current.status != PlaybackStatus.Playing) return current
        return current.copy(status = PlaybackStatus.Paused)
    }

    private fun ended(current: PlaybackState): PlaybackState = current.copy(status = PlaybackStatus.Ended, positionMs = current.durationMs ?: current.positionMs)

    private fun positionChanged(event: PlaybackEvent.PositionChanged, current: PlaybackState): PlaybackState {
        if (current.status != PlaybackStatus.Playing) return current
        return current.copy(positionMs = event.positionMs.coerceAtLeast(0L))
    }

    private fun update(state: PlaybackState) {
        mutableState.value = state
    }
}
