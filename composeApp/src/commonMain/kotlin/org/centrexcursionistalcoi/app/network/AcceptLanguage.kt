package org.centrexcursionistalcoi.app.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * The language of the device as a BCP 47 tag (e.g. `ca-ES`), if it has one.
 */
expect fun systemLanguageTag(): String?

private object LanguageSource : KoinComponent {
    suspend fun current(): String? = get<SettingsStore>().get(SETTINGS_LANGUAGE) ?: systemLanguageTag()
}

/**
 * Sends the language of the app (the one chosen in the settings, or else the one of the device) in the
 * `Accept-Language` of the requests, so the server knows it: it sends the emails in the language of the user.
 */
val AcceptLanguageHeader = createClientPlugin("AcceptLanguageHeader") {
    onRequest { request, _ ->
        if (request.headers.contains(HttpHeaders.AcceptLanguage)) return@onRequest
        LanguageSource.current()?.let { request.header(HttpHeaders.AcceptLanguage, it) }
    }
}
