package org.centrexcursionistalcoi.app.auth

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

/**
 * Persists the credentials used for the last successful login, so [AuthBackend.tryAutoRelogin] can silently
 * re-authenticate after the session expires instead of forcing the user back to the login screen.
 *
 * Backed by Android's `AccountManager` on Android (the OS-blessed place for this exact purpose -- encrypted at
 * rest, scoped to this app's own account type). Not implemented on other platforms yet: [save] is a no-op and
 * [get] always returns `null` there, so [AuthBackend.tryAutoRelogin] always falls through to a normal logout.
 */
@Singleton
expect class CredentialsStore {
    fun save(email: String, password: String)
    fun get(): SavedCredentials?
    fun clear()
}
