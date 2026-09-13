package com.learnhuayu.core.data.db.entity

import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.model.Attempt
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.ConversationSession
import com.learnhuayu.core.model.ConversationSpeaker
import com.learnhuayu.core.model.DebriefEntry
import com.learnhuayu.core.model.DebriefKind
import com.learnhuayu.core.model.FieldMission
import com.learnhuayu.core.model.LocalPersona
import com.learnhuayu.core.model.MissionSession
import com.learnhuayu.core.model.MissionSpeaker
import com.learnhuayu.core.model.MissionTurn
import com.learnhuayu.core.model.Progress
import com.learnhuayu.core.model.RaymondMessage
import com.learnhuayu.core.model.RaymondRole
import com.learnhuayu.core.model.ScriptTurn
import com.learnhuayu.core.model.Turn
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ModelMappingTest {
    private val recordedAt = Instant.parse("2026-09-13T10:15:30Z")

    @Test
    fun attemptRoundTrip() {
        val attempt =
            Attempt(
                id = "attempt-1",
                contentItemId = "tones-ma1",
                recordedAt = recordedAt,
                userAudioRef = "attempts/attempt-1.wav",
                transcript = "ma1",
                feedbackText = "Clear first tone.",
            )

        assertThat(attempt.toEntity().toModel()).isEqualTo(attempt)
        assertThat(attempt.copy(transcript = null, feedbackText = null).toEntity().toModel())
            .isEqualTo(attempt.copy(transcript = null, feedbackText = null))
    }

    @Test
    fun progressRoundTrip() {
        val progress =
            Progress(
                contentItemId = "tones-ma1",
                timesPracticed = 4,
                lastPracticedAt = recordedAt,
                feedbackThemes = listOf("tone 1 pitch drop", "third tone creak"),
            )

        assertThat(progress.toEntity().toModel()).isEqualTo(progress)
        assertThat(progress.copy(lastPracticedAt = null, feedbackThemes = emptyList()).toEntity().toModel())
            .isEqualTo(progress.copy(lastPracticedAt = null, feedbackThemes = emptyList()))
    }

    @Test
    fun conversationSessionRoundTrip() {
        val session =
            ConversationSession(
                id = "session-1",
                lessonId = "tones-06-tone-pairs",
                startedAt = recordedAt,
                endedAt = recordedAt.plusSeconds(240),
                turnCount = 6,
                summary = "Tone pairs held up; second tone drifts.",
            )

        assertThat(session.toEntity().toModel()).isEqualTo(session)
        assertThat(session.copy(endedAt = null, summary = null).toEntity().toModel())
            .isEqualTo(session.copy(endedAt = null, summary = null))
    }

    @Test
    fun turnRoundTrip() {
        val turn =
            Turn(
                id = "turn-1",
                sessionId = "session-1",
                speaker = ConversationSpeaker.AI,
                audioRef = "ai/turn-1.wav",
                transcript = "hen3 hao3",
                feedback = "Reply heard.",
            )

        assertThat(turn.toEntity().toModel()).isEqualTo(turn)
    }

    @Test
    fun fieldMissionRoundTrip() {
        val mission =
            FieldMission(
                id = "mission-1",
                date = LocalDate.parse("2026-09-13"),
                theme = "buying tea",
                scriptTurns =
                listOf(
                    ScriptTurn(pinyin = "ni3 hao3", meaning = "hello", targetTones = listOf(3, 3)),
                    ScriptTurn(pinyin = "duo1 shao3 qian2", meaning = "how much", targetTones = listOf(1, 3, 2)),
                ),
                source = ContentSource.BUNDLED,
                localPersonaIds = listOf("persona-1", "persona-2"),
            )

        assertThat(mission.toEntity().toModel()).isEqualTo(mission)
    }

    @Test
    fun localPersonaRoundTrip() {
        val persona =
            LocalPersona(
                id = "persona-1",
                label = "Tea shop owner",
                settingRole = "shopkeeper",
                personality = "warm, brisk",
                voiceProfile = "kokoro-af-heart",
                pace = "steady",
            )

        assertThat(persona.toEntity().toModel()).isEqualTo(persona)
    }

    @Test
    fun missionSessionRoundTrip() {
        val session =
            MissionSession(
                id = "mission-session-1",
                missionId = "mission-1",
                personaId = "persona-1",
                startedAt = recordedAt,
                endedAt = recordedAt.plusSeconds(90),
                turnCount = 4,
                outcome = "completed",
            )

        assertThat(session.toEntity().toModel()).isEqualTo(session)
    }

    @Test
    fun missionTurnRoundTrip() {
        val turn =
            MissionTurn(
                id = "mission-turn-1",
                sessionId = "mission-session-1",
                speaker = MissionSpeaker.LOCAL,
                audioRef = "mission/turn-1.wav",
                transcript = "shi2 kuai4",
            )

        assertThat(turn.toEntity().toModel()).isEqualTo(turn)
    }

    @Test
    fun debriefEntryRoundTrip() {
        val entry =
            DebriefEntry(
                id = "debrief-1",
                missionId = "mission-1",
                pinyin = "duo1 shao3 qian2",
                kind = DebriefKind.TONE_BREAKDOWN,
                note = "second syllable fell",
                createdAt = recordedAt,
            )

        assertThat(entry.toEntity().toModel()).isEqualTo(entry)
    }

    @Test
    fun raymondMessageRoundTrip() {
        val message =
            RaymondMessage(
                id = "raymond-1",
                role = RaymondRole.RAYMOND,
                text = "Ni3 hao3 is the neutral greeting.",
                audioRef = "raymond/raymond-1.wav",
                createdAt = recordedAt,
            )

        assertThat(message.toEntity().toModel()).isEqualTo(message)
        assertThat(message.copy(audioRef = null).toEntity().toModel()).isEqualTo(message.copy(audioRef = null))
    }
}
