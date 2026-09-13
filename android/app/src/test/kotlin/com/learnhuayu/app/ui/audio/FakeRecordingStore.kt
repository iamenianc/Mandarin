package com.learnhuayu.app.ui.audio

import com.learnhuayu.app.audio.RecordingStore
import java.io.File

class FakeRecordingStore(private val file: File) : RecordingStore {

    var createdCount = 0
        private set

    override fun newRecordingFile(): File {
        createdCount++
        return file
    }
}
