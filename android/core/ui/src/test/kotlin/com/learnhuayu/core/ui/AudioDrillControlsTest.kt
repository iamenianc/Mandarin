package com.learnhuayu.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioDrillControlsTest {

    @Test
    fun `record button labels name each capture state`() {
        assertThat(recordButtonLabel(RecordButtonState.Idle)).isEqualTo("Record")
        assertThat(recordButtonLabel(RecordButtonState.Recording)).isEqualTo("Stop")
        assertThat(recordButtonLabel(RecordButtonState.Processing)).isEqualTo("Processing")
    }

    @Test
    fun `record button state descriptions name the next action`() {
        assertThat(recordButtonStateDescription(RecordButtonState.Idle)).contains("Tap to start")
        assertThat(recordButtonStateDescription(RecordButtonState.Recording)).contains("Tap to stop")
        assertThat(recordButtonStateDescription(RecordButtonState.Processing)).contains("Wait")
    }

    @Test
    fun `record button is clickable except while processing`() {
        assertThat(recordButtonActionEnabled(RecordButtonState.Idle)).isTrue()
        assertThat(recordButtonActionEnabled(RecordButtonState.Recording)).isTrue()
        assertThat(recordButtonActionEnabled(RecordButtonState.Processing)).isFalse()
    }

    @Test
    fun `playback button turns into replay after the first play`() {
        assertThat(playbackButtonLabel(hasPlayed = false)).isEqualTo("Play")
        assertThat(playbackButtonLabel(hasPlayed = true)).isEqualTo("Replay")
    }

    @Test
    fun `playback descriptions distinguish reference from attempt`() {
        assertThat(playbackButtonDescription(hasPlayed = false, playing = false)).contains("reference")
        assertThat(playbackButtonDescription(hasPlayed = true, playing = true)).contains("Playing")
        assertThat(playbackButtonDescriptionForAttempt(hasPlayed = false, playing = false)).contains("your recording")
    }

    @Test
    fun `level meter description names a percent`() {
        assertThat(levelMeterDescription(42)).isEqualTo("Microphone input level 42 percent")
    }
}
