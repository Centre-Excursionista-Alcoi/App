package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import org.centrexcursionistalcoi.app.data.RefreshTokenRequest
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.annotation.Singleton

@Singleton
class AuthBackend(
    private val db: AppDatabase,
    private val credentialsStore: CredentialsStore,
    private val sessionTokens: SessionTokens,
) {

    private val log = logging()
    
    suspend fun register(email: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/register",
            formParameters = parameters {
                append("email", email)
                append("password", password)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Registration successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun login(email: String, password: String) {
        // Clear storage before logging in, so nothing from a previous account is left behind
        settings.clear()

        authenticate(email, password)
    }

    /** Starts a session with [email] and [password], saving its tokens. */
    internal suspend fun authenticate(email: String, password: String) {
        val response = getHttpClient().submitForm(
            url = "/auth/login",
            formParameters = parameters {
                append("email", email)
                append("password", password)
            }
        ) { skipSessionAuth() }
        if (response.status.isSuccess()) {
            log.d { "Login successful." }
            sessionTokens.onLoggedIn(response.body<TokenResponse>())
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    /**
     * Tries to silently get a fresh session from the one saved on this device (see [CredentialsStore]). Used when
     * the server rejects the session, so the user isn't bounced back to the login screen when the session can still be recovered.
     * @return `true` if there's a valid session now; `false` if there was no saved session, or it's no longer
     * valid -- in which case it's already been forgotten, and the caller should fall back to a normal [logout].
     */
    suspend fun tryAutoRelogin(): Boolean {
        if (credentialsStore.getSession() == null) return false
        return try {
            sessionTokens.refresh(getHttpClient()).also { refreshed ->
                if (refreshed) log.d { "Session refreshed." } else log.d { "The saved session is no longer valid." }
            }
        } catch (e: Exception) {
            // No connectivity, a timeout, a server error... say nothing about whether the session is still valid:
            // keep it, so the next attempt can still use it.
            log.w(e) { "Could not refresh the session; keeping it for the next attempt." }
            false
        }
    }

    suspend fun logout() {
        endSession()
        log.d { "Logged out. Removing all data..." }
        clearLocalData()
    }

    /**
     * Revokes the session on the server.
     * @throws Exception if the server couldn't be reached, so that the session isn't forgotten locally while it's
     * still valid.
     */
    private suspend fun endSession() {
        val session = credentialsStore.getSession()
        val response = getHttpClient().post("/auth/logout") {
            skipSessionAuth()
            if (session != null) {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(session.refreshToken))
            }
        }
        if (!response.status.isSuccess()) {
            val error = response.bodyAsError()
            log.d { "Logout failed (${response.status}): $error" }
            throw error.toThrowable()
        }
    }

    /**
     * Wipes the account saved for [AuthBackend.tryAutoRelogin] (see [CredentialsStore]) and all local data, the
     * same as [logout], but without requiring the server to be reachable -- used when the user chooses to forget
     * a previously-saved account straight from the Login screen (e.g. after reaching it with one still saved,
     * see [LoginViewModel]) rather than through a normal in-app logout.
     */
    suspend fun forgetLocalAccount() {
        log.d { "Forgetting locally saved account..." }
        // Best-effort: the session may already be over (that's exactly how the user could end up back on the
        // Login screen with a saved account in the first place).
        try {
            endSession()
        } catch (e: Exception) {
            log.d { "Could not end the session on the server; ignoring: $e" }
        }
        clearLocalData()
    }

    internal suspend fun clearLocalData() {
        // Room handles the foreign-key-safe order itself, unlike deleting through each repository one by one
        // (see DatabaseIntegrityVerifier.clearDatabaseAndResync for the same approach).
        db.clearAllTables()
        log.d { "Removing all files..." }
        // Best-effort: a leftover file (e.g. one FileSystem.deleteRecursively couldn't remove) is stale garbage,
        // not worth failing the rest of this cleanup over -- FCM/settings/credentials must still be cleared, or
        // this whole (suspend, uncaught) call would blow up logout/forgetLocalAccount entirely.
        try {
            FileSystem.deleteAll().also { log.v { "$it files were deleted." } }
        } catch (e: Exception) {
            log.w(e) { "Failed to fully remove local files; continuing with the rest of the local cleanup." }
        }
        log.d { "Revoking FCM token..." }
        FCMTokenManager.revoke()
        log.d { "Removing all settings..." }
        settings.clear()
        credentialsStore.clear()
        sessionTokens.onLoggedOut()
    }

    suspend fun forgotPassword(email: String) {
        val response = getHttpClient().submitForm(
            url = "/lost_password",
            formParameters = parameters {
                append("email", email)
            }
        )
        if (response.status.isSuccess()) {
            log.d { "Forgot password request successful." }
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }

    suspend fun deleteAccount() {
        val response = getHttpClient().post("/delete_account")
        if (response.status.isSuccess()) {
            log.w { "Account delete request successful. Removing all data..." }
            clearLocalData()
        } else {
            throw response.bodyAsError().toThrowable()
        }
    }
}
