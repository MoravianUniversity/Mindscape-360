package edu.moravian.mindscape360

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://mindscape-360.s3.us-east-1.amazonaws.com"

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    expectSuccess = true

    defaultRequest { url(BASE_URL) }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        })
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 15_000      // 15 seconds to establish connection
        socketTimeoutMillis = 60_000       // 60 seconds between data packets (was 2 seconds - too short!)
        requestTimeoutMillis = 300_000     // 5 minutes total for large video downloads (was 60 seconds)
    }

    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.HEADERS
    }
}
