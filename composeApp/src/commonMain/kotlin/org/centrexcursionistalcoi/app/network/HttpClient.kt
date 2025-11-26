package org.centrexcursionistalcoi.app.network

import androidx.annotation.VisibleForTesting
import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage
import org.centrexcursionistalcoi.app.storage.settings

private val log = logging()

private fun createHttpClient(): HttpClient = HttpClient(createHttpClientEngine()) {
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
                log.v { message }
            }
        }
        level = LogLevel.HEADERS
    }
}

/**
 * Adds an `If-Modified-Since` header to the request if a last sync time is stored in settings.
 * @param lastSyncSettingsKey The settings key where the last sync time is stored.
 */
fun HttpRequestBuilder.ifModifiedSince(lastSyncSettingsKey: String) {
    val lastSync = settings.getLongOrNull(lastSyncSettingsKey)

    if (lastSync != null) {
        headers.append(HttpHeaders.IfModifiedSince, HttpDateFormatter.format(lastSync))
    }
}
