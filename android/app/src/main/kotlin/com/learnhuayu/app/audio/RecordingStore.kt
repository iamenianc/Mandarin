package com.learnhuayu.app.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

interface RecordingStore {

    /**
     * Reserves a fresh cache WAV for one capture. A [label] distinguishes recordings from
     * different screens (and per-attempt files within a session) so a new capture never
     * overwrites an earlier one.
     */
    fun newRecordingFile(label: String = DEFAULT_LABEL): File

    companion object {
        const val DEFAULT_LABEL = "audio-check"
    }
}

class CacheRecordingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecordingStore {

    private val sequence = AtomicLong(0L)

    override fun newRecordingFile(label: String): File {
        val prefix = label.takeIf { it.isNotBlank() } ?: "recording"
        val unique = "${System.currentTimeMillis()}-${sequence.incrementAndGet()}"
        val file = File(context.cacheDir, "recordings/$prefix-$unique.wav")
        file.parentFile?.mkdirs()
        return file
    }
}
