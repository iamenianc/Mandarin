package com.learnhuayu.feature.field

import com.learnhuayu.core.ai.MissionLocal
import com.learnhuayu.core.model.ContentSource
import com.learnhuayu.core.model.FieldMission
import com.learnhuayu.core.model.LocalPersona
import com.learnhuayu.core.model.ScriptTurn
import java.time.LocalDate

/**
 * The offline bundled mission (WF-9 fallback, ADR 0013): a bite-sized market greeting exchange
 * with a bundled persona set of five distinct locals. Used whenever mission generation fails
 * or the device is offline, so the loop always works offline (NFR-3). All pinyin carries tone
 * numbers; there is no hanzi anywhere (ADR 0012, NFR-10).
 */
object BundledFieldMission {

    const val MISSION_ID = "bundled-market-greeting"
    const val THEME = "market greeting"

    val script: List<ScriptTurn> = listOf(
        ScriptTurn(
            pinyin = "ni3 hao3",
            meaning = "hello",
            targetTones = listOf(3, 3),
        ),
        ScriptTurn(
            pinyin = "zhe4 ge5 duo1 shao3 qian2",
            meaning = "how much is this",
            targetTones = listOf(4, 5, 1, 3, 2),
        ),
        ScriptTurn(
            pinyin = "tai4 gui4 le5",
            meaning = "too expensive",
            targetTones = listOf(4, 4, 5),
        ),
        ScriptTurn(
            pinyin = "pian2 yi5 yi4 dian3",
            meaning = "a little cheaper",
            targetTones = listOf(2, 5, 2, 4, 3),
        ),
        ScriptTurn(
            pinyin = "xie4 xie5 ni3",
            meaning = "thank you",
            targetTones = listOf(4, 5, 3),
        ),
    )

    val locals: List<LocalPersona> = listOf(
        LocalPersona(
            id = "$MISSION_ID-local-1",
            label = "Tea seller",
            settingRole = "market tea stall owner",
            personality = "warm and chatty",
            voiceProfile = "middle-aged woman, unhurried",
            pace = "slow",
        ),
        LocalPersona(
            id = "$MISSION_ID-local-2",
            label = "Fruit seller",
            settingRole = "market fruit stall owner",
            personality = "brisk and businesslike",
            voiceProfile = "young man, quick",
            pace = "fast",
        ),
        LocalPersona(
            id = "$MISSION_ID-local-3",
            label = "Neighbor",
            settingRole = "shopper browsing nearby",
            personality = "patient and encouraging",
            voiceProfile = "older woman, soft",
            pace = "slow",
        ),
        LocalPersona(
            id = "$MISSION_ID-local-4",
            label = "Delivery rider",
            settingRole = "courier waiting for pickup",
            personality = "impatient but friendly",
            voiceProfile = "young woman, clipped",
            pace = "fast",
        ),
        LocalPersona(
            id = "$MISSION_ID-local-5",
            label = "Night guard",
            settingRole = "market gate guard",
            personality = "calm and formal",
            voiceProfile = "older man, steady",
            pace = "medium",
        ),
    )

    fun mission(date: LocalDate = LocalDate.now()): FieldMission = FieldMission(
        id = MISSION_ID,
        date = date,
        theme = THEME,
        scriptTurns = script,
        source = ContentSource.BUNDLED,
        localPersonaIds = locals.map(LocalPersona::id),
    )

    fun LocalPersona.toMissionLocal(): MissionLocal = MissionLocal(
        label = label,
        settingRole = settingRole,
        personality = personality,
        voiceProfile = voiceProfile,
        pace = pace,
    )
}
