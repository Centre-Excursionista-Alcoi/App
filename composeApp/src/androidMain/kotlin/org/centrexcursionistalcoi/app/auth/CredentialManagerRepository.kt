package org.centrexcursionistalcoi.app.auth

import android.content.Context
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.diamondedge.logging.logging
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.data.RegisterRestoreKeyRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.data.webauthn.AuthenticationOptionsResponse
import org.centrexcursionistalcoi.app.data.webauthn.CreationOptionsResponse
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.koin.core.annotation.Singleton

/**
 * Client side of the server's WebAuthn routes (see `webAuthnRoutes()` on the server), used for Android's Restore
 * Credentials: a restore key is registered after every successful login, and redeemed on a new device to get a
 * session back without the user typing their password again.
 */
@Singleton
class CredentialManagerRepository(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val httpClient = getHttpClient()
    private val log = logging()

    /**
     * Not the shared `settings`: those are wiped on every login, and the id stored here must survive until the
     * next restore key is registered from this device (including after a logout).
     *
     * Excluded from backups (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`): it names *this*
     * device's key, so a device set up from this one's backup must not inherit it.
     */
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * Creates a new restore key in the Credential Manager and registers it on the server.
     * Requires an active session: the server only hands out registration challenges to logged-in users.
     *
     * If E2EE is not available, the key is created without cloud backup (so it only survives device-to-device
     * transfers, not cloud restores).
     *
     * The new key overwrites this device's previous one (Android keeps a single restore key per app), so the
     * previous key's id is sent along for the server to delete its now-unusable record.
     * @throws ServerException if the server rejects the challenge request or the registration.
     * @throws androidx.credentials.exceptions.CreateCredentialException if the Credential Manager fails to create
     * the key (e.g. unsupported device, or no provider available).
     */
    suspend fun create() {
        val challengeResponse = httpClient.post("/generate-restore-challenge")
            .successOrThrow()
            .body<CreationOptionsResponse>()
        val requestJson = webAuthnJson.encodeToString(CreationOptionsResponse.serializer(), challengeResponse)

        val response = try {
            createCredential(requestJson, isCloudBackupEnabled = true)
        } catch (e: E2eeUnavailableException) {
            log.w(e) { "E2EE is not available. Creating credential without cloud backup" }
            createCredential(requestJson, isCloudBackupEnabled = false)
        }

        httpClient.post("/register-restore-key") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRestoreKeyRequest(
                    registrationResponseJson = response.responseJson,
                    replacesCredentialId = preferences.getString(KEY_RESTORE_CREDENTIAL_ID, null),
                )
            )
        }.successOrThrow()

        val credentialId = webAuthnJson.parseToJsonElement(response.responseJson)
            .jsonObject["id"]?.jsonPrimitive?.contentOrNull
            // The server stores ids as Base64Url without padding.
            ?.trimEnd('=')
        preferences.edit { putString(KEY_RESTORE_CREDENTIAL_ID, credentialId) }
    }

    private suspend fun createCredential(
        requestJson: String,
        isCloudBackupEnabled: Boolean,
    ): CreateRestoreCredentialResponse {
        val response = credentialManager.createCredential(
            context,
            CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled),
        )
        return response as? CreateRestoreCredentialResponse
            ?: error("Unexpected credential response type: ${response::class.simpleName}")
    }

    /**
     * Redeems the restore key stored in the Credential Manager (if any) for a new session.
     * @return the new session's tokens.
     * @throws androidx.credentials.exceptions.GetCredentialException if there's no restore key to redeem
     * (`NoCredentialException`), or the Credential Manager fails to retrieve it.
     * @throws ServerException if the server rejects the challenge request or the credential.
     */
    suspend fun recover(): TokenResponse {
        val challengeResponse = httpClient.post("/generate-auth-challenge") { skipSessionAuth() }
            .successOrThrow()
            .body<AuthenticationOptionsResponse>()

        val option = GetRestoreCredentialOption(
            requestJson = webAuthnJson.encodeToString(AuthenticationOptionsResponse.serializer(), challengeResponse),
        )
        val getResponse = credentialManager.getCredential(context, GetCredentialRequest(listOf(option)))
        val credential = getResponse.credential as? RestoreCredential
            ?: error("Unexpected credential type: ${getResponse.credential.type}")

        return httpClient.post("/auth/webauthn/verify") {
            skipSessionAuth()
            contentType(ContentType.Application.Json)
            setBody(RestoreKeyVerificationRequest(credential.authenticationResponseJson))
        }.successOrThrow().body()
    }

    /**
     * Removes the restore key from the Credential Manager, so it can't be used to log back in.
     */
    suspend fun clear() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL))
    }

    private suspend fun HttpResponse.successOrThrow(): HttpResponse {
        if (!status.isSuccess()) throw ServerException.fromResponse(this)
        return this
    }

    private companion object {
        const val PREFERENCES_NAME = "credential_manager"
        const val KEY_RESTORE_CREDENTIAL_ID = "restore_credential_id"

        /**
         * Encodes the server's options into the `requestJson` the Credential Manager expects.
         *
         * [Json]'s defaults would drop every property left at its default value (`pubKeyCredParams`,
         * `authenticatorSelection`, `timeout`, ...), some of which WebAuthn requires, so they're always encoded
         * here; `null`s are omitted instead, since WebAuthn treats a missing member and an explicit `null`
         * differently.
         */
        val webAuthnJson = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    }
}
