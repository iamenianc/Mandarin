package com.learnhuayu.core.audio.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoAudioPlayer @Inject constructor(
    @ApplicationContext context: Context,
) : AudioPlayer,
    Player.Listener {

    private val machine = PlaybackStateMachine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val state: StateFlow<PlaybackState> = machine.state

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true,
        )
        .build()
        .apply { addListener(this@ExoAudioPlayer) }

    init {
        scope.launch {
            while (isActive) {
                if (machine.state.value.status == PlaybackStatus.Playing) {
                    machine.onEvent(PlaybackEvent.PositionChanged(player.currentPosition))
                }
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    override fun play(source: AudioSource) {
        execute(machine.onCommand(PlaybackCommand.Play(source)))
    }

    override fun pause() {
        execute(machine.onCommand(PlaybackCommand.Pause))
    }

    override fun replay() {
        execute(machine.onCommand(PlaybackCommand.Replay))
    }

    override fun seekTo(positionMs: Long) {
        execute(machine.onCommand(PlaybackCommand.Seek(positionMs)))
    }

    override fun release() {
        execute(machine.onCommand(PlaybackCommand.Release))
        scope.cancel()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> machine.onEvent(PlaybackEvent.Prepared(durationOrNull()))
            Player.STATE_ENDED -> machine.onEvent(PlaybackEvent.Ended)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        machine.onEvent(if (isPlaying) PlaybackEvent.StartedPlaying else PlaybackEvent.StoppedPlaying)
    }

    override fun onPlayerError(error: PlaybackException) {
        machine.onEvent(PlaybackEvent.Failed(error.message ?: error.errorCodeName))
    }

    private fun durationOrNull(): Long? = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 }

    private fun execute(effects: List<PlaybackEffect>) {
        for (effect in effects) {
            when (effect) {
                is PlaybackEffect.Load -> {
                    player.setMediaItem(mediaItem(effect.source))
                    player.prepare()
                }
                PlaybackEffect.StartPlayback -> player.play()
                PlaybackEffect.PausePlayback -> player.pause()
                is PlaybackEffect.SeekTo -> player.seekTo(effect.positionMs)
                PlaybackEffect.ReleasePlayer -> {
                    player.removeListener(this)
                    player.release()
                }
            }
        }
    }

    private fun mediaItem(source: AudioSource): MediaItem = when (source) {
        is AudioSource.Asset -> MediaItem.fromUri("$ASSET_URI_PREFIX${source.path.trimStart('/')}")
        is AudioSource.LocalFile -> MediaItem.fromUri(Uri.fromFile(File(source.path)))
        is AudioSource.UriSource -> MediaItem.fromUri(source.uri)
    }

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 100L
        const val ASSET_URI_PREFIX = "asset:///"
    }
}
