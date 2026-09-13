package com.learnhuayu.core.data.db.entity

import com.learnhuayu.core.model.ScriptTurn
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val codecJson = Json { ignoreUnknownKeys = true }

internal fun encodeStringList(values: List<String>): String = codecJson.encodeToString(ListSerializer(String.serializer()), values)

internal fun decodeStringList(raw: String): List<String> = codecJson.decodeFromString(ListSerializer(String.serializer()), raw)

internal fun encodeScriptTurns(turns: List<ScriptTurn>): String = codecJson.encodeToString(ListSerializer(ScriptTurnJson.serializer()), turns.map(ScriptTurn::toJson))

internal fun decodeScriptTurns(raw: String): List<ScriptTurn> = codecJson.decodeFromString(ListSerializer(ScriptTurnJson.serializer()), raw).map(ScriptTurnJson::toModel)

@Serializable
private data class ScriptTurnJson(
    val pinyin: String,
    val meaning: String,
    val targetTones: List<Int>,
)

private fun ScriptTurn.toJson(): ScriptTurnJson = ScriptTurnJson(pinyin, meaning, targetTones)

private fun ScriptTurnJson.toModel(): ScriptTurn = ScriptTurn(pinyin, meaning, targetTones)
