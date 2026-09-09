package org.centrexcursionistalcoi.app.auth

import org.koin.core.annotation.Singleton

data class SavedCredentials(val email: String, val password: String)

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
