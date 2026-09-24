package org.centrexcursionistalcoi.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.diamondedge.logging.logging
import io.ktor.client.call.body
import io.ktor.client.request.post
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.data.webauthn.AuthenticationOptionsResponse
import org.centrexcursionistalcoi.app.data.webauthn.CreationOptionsResponse
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.koin.core.annotation.Singleton

@Singleton
class CredentialManagerRepository(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val httpClient = getHttpClient()
    private val log = logging()

    /**
     * Creates a new credential in the Credential Manager using a challenge from the server.
     * If E2EE is not available, it will try again without cloud backup.
     * @throws IllegalStateException If
     * - the requestJson is invalid or does not follow the WebAuthn format
     * - if the createRestoreRequest is empty or not valid JSON
     * - if it doesn't have a valid user.id that conforms to the WebAuthn specifications
     * - if E2EE is not available and the credential cannot be created without cloud backup
     */
    suspend fun create() {
        val challengeRequest = httpClient.post("/generate-restore-challenge")
        val challengeResponse = challengeRequest.body<CreationOptionsResponse>()
        createCredential(challengeResponse)
    }

    /**
     * Tries to create a credential with the given challenge response.
     * If E2EE is not available, it will try again without cloud backup.
     * @param challengeResponse The challenge response to use for creating the credential.
     * @param isCloudBackupEnabled Whether to enable cloud backup for the credential. Defaults to true.
     * @throws IllegalStateException If
     * - the requestJson is invalid or does not follow the WebAuthn format
     * - if the createRestoreRequest is empty or not valid JSON
     * - if it doesn't have a valid user.id that conforms to the WebAuthn specifications.
     */
    private suspend fun createCredential(
        challengeResponse: CreationOptionsResponse,
        isCloudBackupEnabled: Boolean = true
    ): CreateCredentialResponse {
        try {
            return credentialManager.createCredential(
                context,
                CreateRestoreCredentialRequest(
                    requestJson = Json.encodeToString(
                        CreationOptionsResponse.serializer(),
                        challengeResponse
                    ),
                    isCloudBackupEnabled = isCloudBackupEnabled
                )
            )
        } catch (e: CreateRestoreCredentialDomException) {
            // requestJson is invalid and does not follow the WebAuthn format
            throw IllegalStateException("Invalid requestJson for CreateRestoreCredentialRequest", e)
        } catch (e: E2eeUnavailableException) {
            log.w(e) { "E2EE is not available. Creating credential without cloud backup" }
            return createCredential(challengeResponse, isCloudBackupEnabled = false)
        } catch (e: IllegalArgumentException) {
            // createRestoreRequest is empty or not valid JSON, or if it doesn't have a valid user.id that conforms to the WebAuthn specifications.
            throw IllegalStateException("Invalid requestJson for CreateRestoreCredentialRequest", e)
        }
    }

    /**
     * Recovers the WebAuthn credentials from the Credential Manager, and tries to log in the user with them.
     * If the recovery fails, the user will have to log in manually.
     * If this function doesn't throw an exception, it means the user has been logged in successfully.
     */
    suspend fun recover() {
        val challengeRequest = httpClient.post("/generate-auth-challenge")
        val challengeResponse = challengeRequest.body<AuthenticationOptionsResponse>()

        val options = GetRestoreCredentialOption(
            requestJson = Json.encodeToString(
                AuthenticationOptionsResponse.serializer(),
                challengeResponse
            ),
        )
        val getRequest = GetCredentialRequest(listOf(options))
        val getResponse = credentialManager.getCredential(context, getRequest)

        val credential = getResponse.credential as RestoreCredential
        // TODO: send the credential to the server for verification and login
        // then the cookie will be set and the user will be logged in.
        // The only counter-side to this approach is that AccountManager will no longer have the password for re-authentication.
    }

    suspend fun clear() {
        val clearRequest = ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)
        credentialManager.clearCredentialState(clearRequest)
    }
}
