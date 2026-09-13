package com.learnhuayu.app.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

interface RecordingStore {

    fun newRecordingFile(): File
}

class CacheRecordingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecordingStore {

    override fun newRecordingFile(): File = File(context.cacheDir, RECORDING_RELATIVE_PATH).apply { parentFile?.mkdirs() }

    private companion object {
        const val RECORDING_RELATIVE_PATH = "recordings/audio-check.wav"
    }
}
