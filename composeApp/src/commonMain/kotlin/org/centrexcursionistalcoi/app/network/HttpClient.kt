package org.centrexcursionistalcoi.app.network

import androidx.annotation.VisibleForTesting
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage

private fun createHttpClient(): HttpClient = HttpClient {
    defaultRequest {
        url(BuildKonfig.SERVER_URL)
    }
    install(HttpCookies) {
        storage = SettingsCookiesStorage.Default
    }
    install(ContentNegotiation) {
        json(json)
    }
    configureLogging()
}

@Suppress("ObjectPropertyName")
@VisibleForTesting
var _httpClient: HttpClient? = null
fun getHttpClient(): HttpClient = _httpClient ?: createHttpClient().also { _httpClient = it }

fun HttpClientConfig<*>.configureLogging() {
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Napier.v(message)
            }
        }
        level = LogLevel.HEADERS
    }
}
