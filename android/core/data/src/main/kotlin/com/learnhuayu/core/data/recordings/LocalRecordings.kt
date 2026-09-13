package com.learnhuayu.core.data.recordings

import java.io.File

/**
 * Location of app-local microphone recordings. The recorder writes WAV files here and
 * one-tap deletion removes the whole directory, so both sides share one definition and
 * cannot drift (NFR-4).
 */
object LocalRecordings {
    const val DIRECTORY_NAME = "recordings"

    fun directory(cacheDir: File): File = File(cacheDir, DIRECTORY_NAME)

    fun file(cacheDir: File, fileName: String): File = File(directory(cacheDir), fileName)
}
