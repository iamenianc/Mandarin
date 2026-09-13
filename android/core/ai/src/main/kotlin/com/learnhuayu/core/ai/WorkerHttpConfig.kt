package com.learnhuayu.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class WorkerHttpConfig(
    val baseUrl: String = "",
    val connectTimeoutMillis: Long = 10_000,
    val requestTimeoutMillis: Long = 35_000,
    val socketTimeoutMillis: Long = 35_000,
)

internal val WorkerJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

internal fun workerHttpClient(engine: HttpClientEngine, config: WorkerHttpConfig): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(WorkerJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = config.connectTimeoutMillis
        requestTimeoutMillis = config.requestTimeoutMillis
        socketTimeoutMillis = config.socketTimeoutMillis
    }
}
