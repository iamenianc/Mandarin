package com.learnhuayu.core.audio.capture

import com.learnhuayu.core.audio.pcm.PcmAudio
import com.learnhuayu.core.audio.pcm.PcmMath
import com.learnhuayu.core.audio.vad.EndOfSpeechDetector
import com.learnhuayu.core.audio.vad.VadEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class AudioRecordRecorder internal constructor(
    private val device: PcmCaptureDevice,
    dispatcher: CoroutineDispatcher,
    private val endOfSpeechDetector: EndOfSpeechDetector? = null,
) : AudioRecorder {

    @Inject
    constructor(device: PcmCaptureDevice, endOfSpeechDetector: EndOfSpeechDetector) :
        this(device, Dispatchers.IO, endOfSpeechDetector)

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val buffer = PcmBuffer()
    private val mutableState = MutableStateFlow<RecorderState>(RecorderState.Idle)
    private val mutableLevel = MutableStateFlow(0f)
    private val mutableVadEvents = MutableSharedFlow<VadEvent>(extraBufferCapacity = VAD_BUFFER_CAPACITY)

    override val state: StateFlow<RecorderState> = mutableState.asStateFlow()
    override val level: StateFlow<Float> = mutableLevel.asStateFlow()
    override val vadEvents: Flow<VadEvent> = mutableVadEvents.asSharedFlow()

    private var captureJob: Job? = null

    @Synchronized
    override fun start() {
        if (captureJob?.isActive == true) return
        buffer.clear()
        endOfSpeechDetector?.reset()
        mutableLevel.value = 0f
        mutableState.value = RecorderState.Recording
        captureJob = scope.launch {
            val frame = ShortArray(device.bufferSizeInShorts)
            try {
                device.start()
                while (isActive) {
                    val read = device.read(frame, 0, frame.size)
                    when {
                        read > 0 -> {
                            buffer.append(frame, 0, read)
                            mutableLevel.value = PcmMath.rms(frame, read)
                            val detector = endOfSpeechDetector
                            if (detector != null) {
                                val event = detector.accept(frame, read)
                                if (event != null) {
                                    mutableVadEvents.tryEmit(event)
                                    if (event is VadEvent.SpeechEnded) detector.reset()
                                }
                            }
                        }
                        read < 0 -> {
                            fail("AudioRecord read failed ($read)")
                            return@launch
                        }
                        else -> delay(POLL_INTERVAL_MS)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                fail(error.message ?: error::class.java.simpleName)
            } finally {
                runCatching { device.stop() }
            }
        }
    }

    override suspend fun stop(): PcmAudio {
        val job = captureJob
        captureJob = null
        job?.cancelAndJoin()
        mutableLevel.value = 0f
        if (mutableState.value == RecorderState.Recording) {
            mutableState.value = RecorderState.Idle
        }
        return PcmAudio(buffer.snapshot(), device.sampleRateHz)
    }

    @Synchronized
    override fun release() {
        captureJob?.cancel()
        captureJob = null
        runCatching { device.stop() }
        runCatching { device.release() }
        scope.cancel()
        buffer.clear()
        mutableLevel.value = 0f
        mutableState.value = RecorderState.Idle
    }

    private fun fail(message: String) {
        mutableState.value = RecorderState.Failed(message)
        mutableLevel.value = 0f
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5L
        const val VAD_BUFFER_CAPACITY = 16
    }
}
