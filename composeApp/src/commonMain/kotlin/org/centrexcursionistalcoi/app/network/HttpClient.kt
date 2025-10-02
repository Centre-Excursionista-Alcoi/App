package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage

private fun createHttpClient(): HttpClient = HttpClient {
    defaultRequest {
        url(BuildKonfig.SERVER_URL)
    }
    install(HttpCookies) {
        storage = SettingsCookiesStorage()
    }
    configureLogging()
}

private var httpClient: HttpClient? = null
fun getHttpClient(): HttpClient = httpClient ?: createHttpClient().also { httpClient = it }

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
