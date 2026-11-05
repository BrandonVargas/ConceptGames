package dev.brandonvargas.conceptgames.data.network

import dev.brandonvargas.conceptgames.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClients {
    /** Wikimedia asks API clients to identify themselves: https://meta.wikimedia.org/wiki/User-Agent_policy */
    const val USER_AGENT =
        "ConceptGames/${BuildConfig.VERSION_NAME} (https://github.com/BrandonVargas/ConceptGames) Ktor"

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun create(engine: HttpClientEngine = OkHttp.create()): HttpClient = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            // Generating a whole study pack can take a while on the free tier.
            requestTimeoutMillis = 180_000
            socketTimeoutMillis = 180_000
        }
        defaultRequest { headers.append(HttpHeaders.UserAgent, USER_AGENT) }
    }
}
