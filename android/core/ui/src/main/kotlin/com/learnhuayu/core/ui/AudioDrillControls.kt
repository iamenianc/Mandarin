package com.learnhuayu.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Capture states for [RecordButton]. The button is a stateless control: the caller owns
 * this state and receives clicks.
 */
enum class RecordButtonState {
    Idle,
    Recording,
    Processing,
}

internal fun recordButtonLabel(state: RecordButtonState): String = when (state) {
    RecordButtonState.Idle -> "Record"
    RecordButtonState.Recording -> "Stop"
    RecordButtonState.Processing -> "Processing"
}

internal fun recordButtonStateDescription(state: RecordButtonState): String = when (state) {
    RecordButtonState.Idle -> "Idle"
    RecordButtonState.Recording -> "Recording"
    RecordButtonState.Processing -> "Processing"
}

internal fun recordButtonActionEnabled(state: RecordButtonState): Boolean = state != RecordButtonState.Processing

internal fun playbackButtonLabel(hasPlayed: Boolean): String = if (hasPlayed) "Replay" else "Play"

/**
 * Plays the reference audio, turning into a replay control once audio has played (FR-2).
 */
@Composable
fun PlaybackButton(
    hasPlayed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            imageVector = if (hasPlayed) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Text(text = playbackButtonLabel(hasPlayed))
    }
}

/**
 * Microphone control with unmistakable idle, recording, and processing states
 * (`docs/03-design.md`, NFR-7). Processing disables the button until the caller moves
 * the state on.
 */
@Composable
fun RecordButton(
    state: RecordButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.semantics { stateDescription = recordButtonStateDescription(state) },
        enabled = enabled && recordButtonActionEnabled(state),
        colors = if (state == RecordButtonState.Recording) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        RecordIndicator(state = state)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        Text(text = recordButtonLabel(state))
    }
}

@Composable
private fun RecordIndicator(state: RecordButtonState) {
    when (state) {
        RecordButtonState.Processing -> CircularProgressIndicator(
            modifier = Modifier.size(RecordIndicatorSize),
            color = LearnHuayuStatusColors.Caution,
            strokeWidth = 2.dp,
        )

        else -> Box(
            modifier = Modifier
                .size(RecordIndicatorSize)
                .clip(CircleShape)
                .background(
                    color = LocalContentColor.current.copy(
                        alpha = if (state == RecordButtonState.Recording) 1f else 0.4f,
                    ),
                ),
        )
    }
}

/**
 * Full-width, low-attention tap target for listening drills that require no speaking
 * (FR-10). Large enough to hit without looking, calm enough not to pull attention from
 * the audio.
 */
@Composable
fun LargeTapTarget(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MaterialTheme.spacing.drillTapTargetMinHeight)
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.medium,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private val RecordIndicatorSize = 12.dp

@Preview(name = "Audio drill controls", showBackground = true)
@Composable
private fun AudioDrillControlsPreview() {
    LearnHuayuTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            PlaybackButton(hasPlayed = false, onClick = {})
            PlaybackButton(hasPlayed = true, onClick = {})
            RecordButton(state = RecordButtonState.Idle, onClick = {})
            RecordButton(state = RecordButtonState.Recording, onClick = {})
            RecordButton(state = RecordButtonState.Processing, onClick = {})
            LargeTapTarget(
                label = "ni3 hao3",
                supportingText = "Tap the card to hear it again",
                onClick = {},
            )
        }
    }
}
