package org.centrexcursionistalcoi.app.auth

import android.os.Build
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.diamondedge.logging.logging
import android.content.Context
import org.centrexcursionistalcoi.app.android.CurrentActivity
import org.koin.core.annotation.Singleton

@Singleton
actual class Passkeys(context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val log = logging()

    private val activity get() = CurrentActivity.activity ?: throw PasskeyException("There's no activity to show the sheet over")

    // Passkeys need Android 9: Credential Manager only supports them from there.
    actual val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    actual val deviceName: String = Build.MODEL

    actual suspend fun create(requestJson: String): String? = try {
        val response = credentialManager.createCredential(activity, CreatePublicKeyCredentialRequest(requestJson))
        (response as CreatePublicKeyCredentialResponse).registrationResponseJson
    } catch (_: CreateCredentialCancellationException) {
        null
    } catch (e: CreateCredentialException) {
        throw PasskeyException(e.errorMessage?.toString() ?: e.type, e)
    }

    actual suspend fun createAutomatically(requestJson: String): String? = try {
        val request = CreatePublicKeyCredentialRequest(requestJson = requestJson, isConditional = true)
        val response = credentialManager.createCredential(activity, request)
        (response as CreatePublicKeyCredentialResponse).registrationResponseJson
    } catch (e: Exception) {
        // Expected whenever the provider doesn't upgrade the account on its own.
        log.d { "No passkey created automatically: $e" }
        null
    }

    actual suspend fun signIn(requestJson: String): SavedCredential? = try {
        val request = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(requestJson), GetPasswordOption()))
        when (val credential = credentialManager.getCredential(activity, request).credential) {
            is PublicKeyCredential -> SavedCredential.Passkey(credential.authenticationResponseJson)
            is PasswordCredential -> SavedCredential.Password(credential.id, credential.password)
            else -> throw PasskeyException("Unexpected credential type: ${credential.type}")
        }
    } catch (_: GetCredentialCancellationException) {
        null
    } catch (_: NoCredentialException) {
        null
    } catch (e: GetCredentialException) {
        throw PasskeyException(e.errorMessage?.toString() ?: e.type, e)
    }

    actual suspend fun savePassword(email: String, password: String) {
        try {
            credentialManager.createCredential(activity, CreatePasswordRequest(email, password))
        } catch (e: Exception) {
            // Declined, or no password manager: nothing to do.
            log.d { "Password not saved: $e" }
        }
    }
}
