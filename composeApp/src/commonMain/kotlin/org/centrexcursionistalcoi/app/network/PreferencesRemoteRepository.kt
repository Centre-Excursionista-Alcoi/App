package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdatePreferencesRequest
import org.centrexcursionistalcoi.app.response.PreferencesResponse
import org.centrexcursionistalcoi.app.routes.Api
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

/**
 * The preferences the server keeps for the user. They are also kept in the app, so nothing here is important enough
 * to fail for: none of these ever throws, a request that doesn't succeed is just as if it hadn't been made.
 */
@Singleton
class PreferencesRemoteRepository : KoinComponent {
    private val log = logging()

    private val httpClient by lazy { getHttpClient() }

    private suspend fun <T> silently(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.w(e) { "Could not $what the preferences" }
        null
    }

    /**
     * The language the server has for the user, as a BCP 47 tag, or `null` if it has none or couldn't be fetched.
     */
    suspend fun getLanguage(): String? = silently("fetch") {
        val response = httpClient.get(Api.Profile.Preferences())
        if (!response.status.isSuccess()) return@silently null
        json.decodeFromString(PreferencesResponse.serializer(), response.bodyAsText()).language
    }

    /**
     * Tells the server the language of the user, as a BCP 47 tag.
     * @return `false` if it couldn't.
     */
    suspend fun setLanguage(tag: String): Boolean = silently("update") {
        httpClient.patch(Api.Profile.Preferences()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdatePreferencesRequest.serializer(), UpdatePreferencesRequest(language = tag)))
        }.status.isSuccess()
    } ?: false
}
