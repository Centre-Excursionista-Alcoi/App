package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.annotation.Singleton

private const val KEY_EMAIL = "auth.email"
private const val KEY_REFRESH_TOKEN = "auth.refresh_token"

/**
 * Keeps the session in the app's settings, the same place (and protection) as the session cookie before tokens.
 * No password was ever saved on desktop, so there's nothing to migrate.
 */
@Singleton
actual class CredentialsStore {
    actual val current: StateFlow<SavedAccount?>
        field = MutableStateFlow(readAccount())

    actual fun saveSession(email: String, refreshToken: String) {
        settings.putString(KEY_EMAIL, email)
        settings.putString(KEY_REFRESH_TOKEN, refreshToken)
        current.value = readAccount()
    }

    actual fun getSession(): SavedSession? {
        val email = settings.getStringOrNull(KEY_EMAIL) ?: return null
        val refreshToken = settings.getStringOrNull(KEY_REFRESH_TOKEN) ?: return null
        return SavedSession(email, refreshToken)
    }

    actual fun getLegacyCredentials(): SavedCredentials? = null

    actual fun clear() {
        settings.remove(KEY_EMAIL)
        settings.remove(KEY_REFRESH_TOKEN)
        current.value = readAccount()
    }

    private fun readAccount(): SavedAccount? = settings.getStringOrNull(KEY_EMAIL)?.let(::SavedAccount)
}
