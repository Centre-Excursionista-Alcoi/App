package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton

class SavedCredentials(val email: String, val password: CharArray) {
    // A data class' auto-generated toString() would print password's contents directly if it were a String;
    // CharArray's default (identity-based) toString() avoids that, so this is deliberately a plain class with
    // hand-written equals()/hashCode() (CharArray.equals() is reference equality, not content equality) rather
    // than a data class.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedCredentials) return false
        return email == other.email && password.contentEquals(other.password)
    }

    override fun hashCode(): Int = 31 * email.hashCode() + password.contentHashCode()
}

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
 * session expired: [getLegacyCredentials] reads it only to migrate to a session (see
 * [LegacyAuthMigration]), and [saveSession] deletes it.
 *
 * [current] is the saved account as a hot, live [StateFlow], for UI code (e.g. the Login screen's "you already have
 * an account saved" warning) that wants to react to it changing over time rather than polling.
 */
@Singleton
expect class CredentialsStore {
    val current: StateFlow<SavedAccount?>

    /** Saves the session of [email], replacing any other saved account and any legacy password. */
    fun saveSession(email: String, refreshToken: String)

    fun getSession(): SavedSession?

    /** The password saved by versions before token authentication, if it hasn't been migrated yet. */
    fun getLegacyCredentials(): SavedCredentials?

    /** Forgets the saved account: its session, and its legacy password if any. */
    fun clear()
}
