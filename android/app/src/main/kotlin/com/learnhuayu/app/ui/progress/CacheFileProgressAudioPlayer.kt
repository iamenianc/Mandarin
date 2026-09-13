package com.learnhuayu.app.ui.progress

import android.content.Context
import com.learnhuayu.core.audio.playback.AudioPlayer
import com.learnhuayu.core.audio.playback.AudioSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes WF-3 speech bytes to the app cache and plays them through the shared audio player,
 * so a voiced progress summary reuses the same playback path as reference and reply audio.
 */
@Singleton
class CacheFileProgressAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioPlayer: AudioPlayer,
) : ProgressAudioPlayer {

    override suspend fun playSpeech(bytes: ByteArray, contentType: String?) {
        val extension = when {
            contentType?.contains("mpeg", ignoreCase = true) == true -> "mp3"
            contentType?.contains("ogg", ignoreCase = true) == true -> "ogg"
            contentType?.contains("mp4", ignoreCase = true) == true -> "m4a"
            else -> "wav"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val name = "progress-speech-" + digest.joinToString("") { "%02x".format(it) } + ".$extension"
        val file = withContext(Dispatchers.IO) {
            val target = File(context.cacheDir, "progress-speech/$name")
            target.parentFile?.mkdirs()
            if (!target.exists()) target.writeBytes(bytes)
            target
        }
        audioPlayer.play(AudioSource.LocalFile(file.absolutePath))
    }
}
