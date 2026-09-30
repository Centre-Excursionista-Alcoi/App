package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton

/** The account saved on this device. */
class SavedAccount(val email: String)

/** A logged-in session saved on this device, see [CredentialsStore]. */
class SavedSession(val email: String, val refreshToken: String) {
    override fun toString(): String = "SavedSession(email=$email, refreshToken=<redacted>)"
}

/**
 * Persists the session of the logged-in account: its refresh token (see `SessionTokens`), which is what keeps the
 * user logged in across app restarts. The access token is only ever kept in memory.
 *
 * Backed by Android's `AccountManager` (the OS-blessed place for this exact purpose -- encrypted at rest,
 * scoped to this app's own account type) and iOS's Keychain (`kSecClassGenericPassword`, only readable on this
 * device); in the app's settings on desktop/JVM.
 *
 * Versions before token authentication saved the account's password instead, to log in again silently once the
 * session expired. It's no longer used: [saveSession] and [clear] delete it.
 *
 * [current] is the saved account as a hot, live [StateFlow], for UI code (e.g. the Login screen's "you already have
 * an account saved" warning) that wants to react to it changing over time rather than polling.
 */
@Singleton
expect class CredentialsStore {
    val current: StateFlow<SavedAccount?>

    /** Saves the session of [email], replacing any other saved account and any legacy password. */
    suspend fun saveSession(email: String, refreshToken: String)

    suspend fun getSession(): SavedSession?

    /** Forgets the saved account: its session, and its legacy password if any. */
    suspend fun clear()
}
