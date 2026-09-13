package com.learnhuayu.core.audio.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackStateMachineTest {

    private val asset = AudioSource.Asset("audio/tones/ma1.wav")
    private val otherAsset = AudioSource.Asset("audio/tones/ma4.wav")

    @Test
    fun `starts idle`() {
        val machine = PlaybackStateMachine()

        assertThat(machine.state.value).isEqualTo(PlaybackState())
    }

    @Test
    fun `play loads a new source and starts playback`() {
        val machine = PlaybackStateMachine()

        val effects = machine.onCommand(PlaybackCommand.Play(asset))

        assertThat(effects).containsExactly(PlaybackEffect.Load(asset), PlaybackEffect.StartPlayback).inOrder()
        assertThat(machine.state.value).isEqualTo(
            PlaybackState(source = asset, status = PlaybackStatus.Buffering),
        )
    }

    @Test
    fun `prepared and started playing move through buffering to playing`() {
        val machine = PlaybackStateMachine()

        machine.onCommand(PlaybackCommand.Play(asset))
        machine.onEvent(PlaybackEvent.Prepared(1_200L))
        machine.onEvent(PlaybackEvent.StartedPlaying)

        assertThat(machine.state.value).isEqualTo(
            PlaybackState(source = asset, status = PlaybackStatus.Playing, positionMs = 0L, durationMs = 1_200L),
        )
    }

    @Test
    fun `position changes are observed while playing`() {
        val machine = playingMachine()

        machine.onEvent(PlaybackEvent.PositionChanged(640L))

        assertThat(machine.state.value.positionMs).isEqualTo(640L)
    }

    @Test
    fun `position changes are ignored while paused`() {
        val machine = playingMachine()
        machine.onCommand(PlaybackCommand.Pause)

        machine.onEvent(PlaybackEvent.PositionChanged(640L))

        assertThat(machine.state.value.positionMs).isEqualTo(0L)
    }

    @Test
    fun `pause pauses a playing source`() {
        val machine = playingMachine()

        val effects = machine.onCommand(PlaybackCommand.Pause)

        assertThat(effects).containsExactly(PlaybackEffect.PausePlayback)
        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Paused)
    }

    @Test
    fun `pause is ignored while idle`() {
        val machine = PlaybackStateMachine()

        val effects = machine.onCommand(PlaybackCommand.Pause)

        assertThat(effects).isEmpty()
        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Idle)
    }

    @Test
    fun `play resumes a paused source without reloading`() {
        val machine = playingMachine()
        machine.onCommand(PlaybackCommand.Pause)

        val effects = machine.onCommand(PlaybackCommand.Play(asset))

        assertThat(effects).containsExactly(PlaybackEffect.StartPlayback)
        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Buffering)
    }

    @Test
    fun `play while playing is ignored`() {
        val machine = playingMachine()

        val effects = machine.onCommand(PlaybackCommand.Play(asset))

        assertThat(effects).isEmpty()
        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Playing)
    }

    @Test
    fun `play of a different source loads it`() {
        val machine = playingMachine()

        val effects = machine.onCommand(PlaybackCommand.Play(otherAsset))

        assertThat(effects).containsExactly(PlaybackEffect.Load(otherAsset), PlaybackEffect.StartPlayback).inOrder()
        assertThat(machine.state.value.source).isEqualTo(otherAsset)
    }

    @Test
    fun `replay restarts the current source`() {
        val machine = playingMachine()
        machine.onEvent(PlaybackEvent.Ended)

        val effects = machine.onCommand(PlaybackCommand.Replay)

        assertThat(effects).containsExactly(PlaybackEffect.SeekTo(0L), PlaybackEffect.StartPlayback).inOrder()
        assertThat(machine.state.value).isEqualTo(
            PlaybackState(source = asset, status = PlaybackStatus.Buffering, positionMs = 0L, durationMs = 1_000L),
        )
    }

    @Test
    fun `replay without a source does nothing`() {
        val machine = PlaybackStateMachine()

        val effects = machine.onCommand(PlaybackCommand.Replay)

        assertThat(effects).isEmpty()
        assertThat(machine.state.value).isEqualTo(PlaybackState())
    }

    @Test
    fun `seek clamps to the known duration and leaves ended state paused`() {
        val machine = playingMachine()
        machine.onEvent(PlaybackEvent.Ended)

        val effects = machine.onCommand(PlaybackCommand.Seek(5_000L))

        assertThat(effects).containsExactly(PlaybackEffect.SeekTo(1_000L))
        assertThat(machine.state.value.positionMs).isEqualTo(1_000L)
        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Paused)
    }

    @Test
    fun `seek clamps negative positions to zero`() {
        val machine = playingMachine()

        val effects = machine.onCommand(PlaybackCommand.Seek(-250L))

        assertThat(effects).containsExactly(PlaybackEffect.SeekTo(0L))
        assertThat(machine.state.value.positionMs).isEqualTo(0L)
    }

    @Test
    fun `seek before loading does nothing`() {
        val machine = PlaybackStateMachine()

        val effects = machine.onCommand(PlaybackCommand.Seek(500L))

        assertThat(effects).isEmpty()
        assertThat(machine.state.value).isEqualTo(PlaybackState())
    }

    @Test
    fun `playback end moves the position to the duration`() {
        val machine = playingMachine()

        machine.onEvent(PlaybackEvent.Ended)

        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Ended)
        assertThat(machine.state.value.positionMs).isEqualTo(1_000L)
    }

    @Test
    fun `stopping playback without a pause command surfaces as paused`() {
        val machine = playingMachine()

        machine.onEvent(PlaybackEvent.StoppedPlaying)

        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Paused)
    }

    @Test
    fun `errors surface as a failed status`() {
        val machine = playingMachine()

        machine.onEvent(PlaybackEvent.Failed("decoder error"))

        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Failed("decoder error"))
    }

    @Test
    fun `play after a failure reloads the source`() {
        val machine = playingMachine()
        machine.onEvent(PlaybackEvent.Failed("decoder error"))

        val effects = machine.onCommand(PlaybackCommand.Play(asset))

        assertThat(effects).containsExactly(PlaybackEffect.Load(asset), PlaybackEffect.StartPlayback).inOrder()
        assertThat(machine.state.value.status).isEqualTo(PlaybackStatus.Buffering)
    }

    @Test
    fun `release resets the state and releases the player`() {
        val machine = playingMachine()

        val effects = machine.onCommand(PlaybackCommand.Release)

        assertThat(effects).containsExactly(PlaybackEffect.ReleasePlayer)
        assertThat(machine.state.value).isEqualTo(PlaybackState())
    }

    private fun playingMachine(): PlaybackStateMachine = PlaybackStateMachine().apply {
        onCommand(PlaybackCommand.Play(asset))
        onEvent(PlaybackEvent.Prepared(1_000L))
        onEvent(PlaybackEvent.StartedPlaying)
    }
}
