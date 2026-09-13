package com.learnhuayu.core.data.prefs

import com.learnhuayu.core.data.content.ContentAssetException
import com.learnhuayu.core.data.content.ContentSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface PreferencesDefaultsSource {
    suspend fun defaults(): PreferencesDefaults
}

@Singleton
internal class BundledPreferencesDefaultsSource @Inject constructor(
    private val source: ContentSource,
    private val json: Json,
) : PreferencesDefaultsSource {
    private val lock = Mutex()
    private var cached: PreferencesDefaults? = null

    override suspend fun defaults(): PreferencesDefaults = lock.withLock {
        cached ?: readDefaults().also { loaded -> cached = loaded }
    }

    private fun readDefaults(): PreferencesDefaults {
        val text =
            try {
                source.readText(DEFAULTS_PATH)
            } catch (error: IOException) {
                throw ContentAssetException("Cannot read preferences defaults: $DEFAULTS_PATH", error)
            }
        return try {
            parsePreferencesDefaults(json, text)
        } catch (error: SerializationException) {
            throw ContentAssetException("Cannot parse preferences defaults: $DEFAULTS_PATH", error)
        }
    }
}

private const val DEFAULTS_PATH = "config/defaults.json"
