package org.centrexcursionistalcoi.app.auth

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.network.getHttpClient

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
                .appendPathSegments("login")
                .apply {
                    parameters["code"] = code
                    parameters["code_verifier"] = codeVerifier
                }
                .build()
        )
        println(response.bodyAsText())
    }
}
