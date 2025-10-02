package org.centrexcursionistalcoi.app.auth

import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage
import org.centrexcursionistalcoi.app.storage.settings

@OptIn(ExperimentalUuidApi::class)
object AuthCallbackProcessor {
    suspend fun processCallbackUrl(url: Url) {
        val query = url.parameters
        val code = query["code"]
        val state = query["state"]

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            throw IllegalArgumentException("Invalid callback URL: missing code or state")
        }

        val stateUuid = try {
            Uuid.parse(state)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid callback URL: state is not a valid UUID", e)
        }

        val codeVerifier = retrieveAndRemoveCodeVerifier(stateUuid)
        if (codeVerifier == null) {
            throw IllegalStateException("No code verifier found for state: $state")
        }

        val response = getHttpClient().get(
            URLBuilder(BuildKonfig.SERVER_URL)
                .appendPathSegments("callback")
                .apply {
                    parameters["code"] = code
                    parameters["code_verifier"] = codeVerifier
                    parameters["redirect_uri"] = BuildKonfig.REDIRECT_URI ?: error("REDIRECT_URI is not set")
                }
                .build()
        )
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Authentication failed: ${response.status}")
        }
        if (!SettingsCookiesStorage.Default.contains("USER_SESSION")) {
            throw IllegalStateException("Authentication failed: USER_SESSION cookie not found")
        }
    }
}
