package org.centrexcursionistalcoi.app.network

import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.data.AddPasskeyRequest
import org.centrexcursionistalcoi.app.data.Reauthentication
import org.centrexcursionistalcoi.app.data.ReauthenticatedRequest
import org.centrexcursionistalcoi.app.data.SecurityInfo
import org.centrexcursionistalcoi.app.data.SetPasswordRequest
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.routes.Api
import org.koin.core.annotation.Singleton

/** How the logged-in user signs in: their passkeys and password. */
@Singleton
class SecurityRemoteRepository {
    private val httpClient get() = getHttpClient()

    private suspend fun HttpResponse.orThrow(): HttpResponse {
        if (!status.isSuccess()) throw bodyAsError().toThrowable()
        return this
    }

    suspend fun getSecurityInfo(): SecurityInfo = httpClient.get(Api.Profile.Security()).orThrow().body()

    /** The WebAuthn creation options for a new passkey, for [org.centrexcursionistalcoi.app.auth.Passkeys.create]. */
    suspend fun passkeyCreationOptions(): String = httpClient.post(Api.Profile.Passkeys.Options()).orThrow().bodyAsText()

    /** Adds the passkey of [registrationResponseJson], created for the options of [passkeyCreationOptions]. */
    suspend fun addPasskey(registrationResponseJson: String, name: String) {
        httpClient.post(Api.Profile.Passkeys()) {
            contentType(ContentType.Application.Json)
            setBody(AddPasskeyRequest(registrationResponseJson, name))
        }.orThrow()
    }

    suspend fun removePasskey(id: String, reauthentication: Reauthentication) {
        httpClient.delete(Api.Profile.Passkeys.Id(id)) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(reauthentication))
        }.orThrow()
    }

    /** Sets a password, or changes it. */
    suspend fun setPassword(newPassword: String, reauthentication: Reauthentication) {
        httpClient.put(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(SetPasswordRequest(reauthentication, newPassword))
        }.orThrow()
    }

    suspend fun removePassword(reauthentication: Reauthentication) {
        httpClient.delete(Api.Profile.Password()) {
            contentType(ContentType.Application.Json)
            setBody(ReauthenticatedRequest(reauthentication))
        }.orThrow()
    }
}
