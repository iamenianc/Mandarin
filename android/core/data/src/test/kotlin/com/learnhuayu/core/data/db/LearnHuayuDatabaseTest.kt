package com.learnhuayu.core.data.db

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.learnhuayu.core.data.db.entity.AttemptEntity
import com.learnhuayu.core.data.db.entity.ConversationSessionEntity
import com.learnhuayu.core.data.db.entity.DebriefEntryEntity
import com.learnhuayu.core.data.db.entity.FieldMissionEntity
import com.learnhuayu.core.data.db.entity.LocalPersonaEntity
import com.learnhuayu.core.data.db.entity.MissionSessionEntity
import com.learnhuayu.core.data.db.entity.MissionTurnEntity
import com.learnhuayu.core.data.db.entity.ProgressEntity
import com.learnhuayu.core.data.db.entity.RaymondMessageEntity
import com.learnhuayu.core.data.db.entity.TurnEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LearnHuayuDatabaseTest {
    private lateinit var database: LearnHuayuDatabase

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                LearnHuayuDatabase::class.java,
            ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun attemptDaoCrud() = runTest {
        val dao = database.attemptDao()
        val attempt =
            AttemptEntity(
                id = "attempt-1",
                contentItemId = "tones-ma1",
                recordedAt = 1_726_000_000_000,
                userAudioRef = "attempts/attempt-1.wav",
                transcript = null,
                feedbackText = null,
            )

        dao.upsert(attempt)
        assertThat(dao.byId("attempt-1")).isEqualTo(attempt)
        assertThat(dao.byContentItem("tones-ma1").first()).containsExactly(attempt)

        val updated = attempt.copy(transcript = "ma1", feedbackText = "Clear first tone.")
        dao.upsert(updated)
        assertThat(dao.byId("attempt-1")).isEqualTo(updated)

        dao.deleteById("attempt-1")
        assertThat(dao.byId("attempt-1")).isNull()
        assertThat(dao.all().first()).isEmpty()
    }

    @Test
    fun progressDaoCrud() = runTest {
        val dao = database.progressDao()
        val progress =
            ProgressEntity(
                contentItemId = "tones-ma1",
                timesPracticed = 3,
                lastPracticedAt = 1_726_000_000_000,
                feedbackThemesJson = """["tone 1 pitch drop"]""",
            )

        dao.upsert(progress)
        assertThat(dao.byId("tones-ma1")).isEqualTo(progress)
        assertThat(dao.all().first()).containsExactly(progress)

        dao.upsert(progress.copy(timesPracticed = 4))
        assertThat(dao.byId("tones-ma1")?.timesPracticed).isEqualTo(4)

        dao.deleteById("tones-ma1")
        assertThat(dao.byId("tones-ma1")).isNull()
    }

    @Test
    fun conversationSessionDaoCrud() = runTest {
        val dao = database.conversationSessionDao()
        val session =
            ConversationSessionEntity(
                id = "session-1",
                lessonId = "tones-01-first-tone",
                startedAt = 1_726_000_000_000,
                endedAt = null,
                turnCount = 0,
                summary = null,
            )

        dao.upsert(session)
        assertThat(dao.byId("session-1")).isEqualTo(session)
        assertThat(dao.all().first()).containsExactly(session)

        val ended = session.copy(endedAt = 1_726_000_600_000, turnCount = 12, summary = "steady")
        dao.upsert(ended)
        assertThat(dao.byId("session-1")).isEqualTo(ended)

        dao.deleteById("session-1")
        assertThat(dao.byId("session-1")).isNull()
    }

    @Test
    fun turnDaoCrudAndCascade() = runTest {
        val sessionDao = database.conversationSessionDao()
        val dao = database.turnDao()
        sessionDao.upsert(
            ConversationSessionEntity(
                id = "session-1",
                lessonId = "tones-01-first-tone",
                startedAt = 1_726_000_000_000,
                endedAt = null,
                turnCount = 0,
                summary = null,
            ),
        )
        val turn =
            TurnEntity(
                id = "turn-1",
                sessionId = "session-1",
                speaker = "USER",
                audioRef = "attempts/turn-1.wav",
                transcript = "ma1",
                feedback = null,
            )

        dao.upsert(turn)
        assertThat(dao.byId("turn-1")).isEqualTo(turn)
        assertThat(dao.bySession("session-1").first()).containsExactly(turn)

        dao.upsert(turn.copy(feedback = "Good first tone."))
        assertThat(dao.byId("turn-1")?.feedback).isEqualTo("Good first tone.")

        sessionDao.deleteById("session-1")
        assertThat(dao.byId("turn-1")).isNull()
    }

    @Test
    fun fieldMissionDaoCrud() = runTest {
        val dao = database.fieldMissionDao()
        val mission =
            FieldMissionEntity(
                id = "mission-1",
                date = "2026-09-13",
                theme = "buying tea",
                scriptTurnsJson =
                """[{"pinyin":"ni3 hao3","meaning":"hello","targetTones":[3,3]}]""",
                source = "BUNDLED",
                localPersonaIdsJson = """["persona-1","persona-2"]""",
            )

        dao.upsert(mission)
        assertThat(dao.byId("mission-1")).isEqualTo(mission)
        assertThat(dao.all().first()).containsExactly(mission)

        dao.upsert(mission.copy(theme = "buying coffee"))
        assertThat(dao.byId("mission-1")?.theme).isEqualTo("buying coffee")

        dao.deleteById("mission-1")
        assertThat(dao.byId("mission-1")).isNull()
    }

    @Test
    fun localPersonaDaoCrud() = runTest {
        val dao = database.localPersonaDao()
        val persona =
            LocalPersonaEntity(
                id = "persona-1",
                label = "Tea shop owner",
                settingRole = "shopkeeper",
                personality = "warm, brisk",
                voiceProfile = "kokoro-af-heart",
                pace = "steady",
            )

        dao.upsert(persona)
        assertThat(dao.byId("persona-1")).isEqualTo(persona)
        assertThat(dao.all().first()).containsExactly(persona)

        dao.upsert(persona.copy(pace = "slow"))
        assertThat(dao.byId("persona-1")?.pace).isEqualTo("slow")

        dao.deleteById("persona-1")
        assertThat(dao.byId("persona-1")).isNull()
    }

    @Test
    fun missionSessionDaoCrud() = runTest {
        val missionDao = database.fieldMissionDao()
        val dao = database.missionSessionDao()
        missionDao.upsert(missionEntity())
        val session =
            MissionSessionEntity(
                id = "mission-session-1",
                missionId = "mission-1",
                personaId = "persona-1",
                startedAt = 1_726_000_000_000,
                endedAt = null,
                turnCount = 0,
                outcome = null,
            )

        dao.upsert(session)
        assertThat(dao.byId("mission-session-1")).isEqualTo(session)
        assertThat(dao.byMission("mission-1").first()).containsExactly(session)

        dao.upsert(session.copy(turnCount = 5, outcome = "completed"))
        assertThat(dao.byId("mission-session-1")?.outcome).isEqualTo("completed")

        dao.deleteById("mission-session-1")
        assertThat(dao.byId("mission-session-1")).isNull()
    }

    @Test
    fun missionTurnDaoCrudAndCascade() = runTest {
        val missionDao = database.fieldMissionDao()
        val sessionDao = database.missionSessionDao()
        val dao = database.missionTurnDao()
        missionDao.upsert(missionEntity())
        sessionDao.upsert(
            MissionSessionEntity(
                id = "mission-session-1",
                missionId = "mission-1",
                personaId = "persona-1",
                startedAt = 1_726_000_000_000,
                endedAt = null,
                turnCount = 0,
                outcome = null,
            ),
        )
        val turn =
            MissionTurnEntity(
                id = "mission-turn-1",
                sessionId = "mission-session-1",
                speaker = "LOCAL",
                audioRef = "mission/turn-1.wav",
                transcript = "shi2 kuai4",
            )

        dao.upsert(turn)
        assertThat(dao.byId("mission-turn-1")).isEqualTo(turn)
        assertThat(dao.bySession("mission-session-1").first()).containsExactly(turn)

        sessionDao.deleteById("mission-session-1")
        assertThat(dao.byId("mission-turn-1")).isNull()
    }

    @Test
    fun debriefEntryDaoCrud() = runTest {
        val missionDao = database.fieldMissionDao()
        val dao = database.debriefEntryDao()
        missionDao.upsert(missionEntity())
        val entry =
            DebriefEntryEntity(
                id = "debrief-1",
                missionId = "mission-1",
                pinyin = "duo1 shao3 qian2",
                kind = "TONE_BREAKDOWN",
                note = "second syllable fell",
                createdAt = 1_726_000_000_000,
            )

        dao.upsert(entry)
        assertThat(dao.byId("debrief-1")).isEqualTo(entry)
        assertThat(dao.byMission("mission-1").first()).containsExactly(entry)
        assertThat(dao.all().first()).containsExactly(entry)

        dao.upsert(entry.copy(note = "check tone 3"))
        assertThat(dao.byId("debrief-1")?.note).isEqualTo("check tone 3")

        dao.deleteById("debrief-1")
        assertThat(dao.byId("debrief-1")).isNull()
    }

    @Test
    fun raymondMessageDaoCrud() = runTest {
        val dao = database.raymondMessageDao()
        val message =
            RaymondMessageEntity(
                id = "raymond-1",
                role = "USER",
                text = "What does ma5 mean?",
                audioRef = null,
                createdAt = 1_726_000_000_000,
            )

        dao.upsert(message)
        assertThat(dao.byId("raymond-1")).isEqualTo(message)
        assertThat(dao.all().first()).containsExactly(message)

        dao.upsert(message.copy(text = "What does the neutral tone mean?"))
        assertThat(dao.byId("raymond-1")?.text).isEqualTo("What does the neutral tone mean?")

        dao.deleteById("raymond-1")
        assertThat(dao.byId("raymond-1")).isNull()
    }

    private fun missionEntity(): FieldMissionEntity = FieldMissionEntity(
        id = "mission-1",
        date = "2026-09-13",
        theme = "buying tea",
        scriptTurnsJson = """[{"pinyin":"ni3 hao3","meaning":"hello","targetTones":[3,3]}]""",
        source = "BUNDLED",
        localPersonaIdsJson = """["persona-1"]""",
    )
}
