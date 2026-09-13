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
    fun `record button state descriptions support TalkBack`() {
        RecordButtonState.entries.forEach { state ->
            assertThat(recordButtonStateDescription(state)).isNotEmpty()
        }
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
}
