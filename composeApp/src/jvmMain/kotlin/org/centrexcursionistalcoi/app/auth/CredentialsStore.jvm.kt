package org.centrexcursionistalcoi.app.auth

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.annotation.Singleton

// TODO: This should be stored on a safe storage
private val KEY_EMAIL = stringPreferencesKey("auth.email")
private val KEY_REFRESH_TOKEN = stringPreferencesKey("auth.refresh_token")

/**
 * Keeps the session in the app's settings, the same place (and protection) as the session cookie before tokens.
 * No password was ever saved on desktop, so there's nothing to migrate.
 */
@Singleton
actual class CredentialsStore(
    private val settings: SettingsStore
) {
    actual val current: StateFlow<SavedAccount?>
        // TODO: Probably we shouldn't use runBlocking
        field = MutableStateFlow(runBlocking { readAccount() })

    actual suspend  fun saveSession(email: String, refreshToken: String) {
        settings.set(KEY_EMAIL, email)
        settings.set(KEY_REFRESH_TOKEN, refreshToken)
        current.value = readAccount()
    }

    actual suspend fun getSession(): SavedSession? {
        val email = settings.get(KEY_EMAIL) ?: return null
        val refreshToken = settings.get(KEY_REFRESH_TOKEN) ?: return null
        return SavedSession(email, refreshToken)
    }

    actual fun getLegacyCredentials(): SavedCredentials? = null

    actual suspend fun clear() {
        settings.remove(KEY_EMAIL)
        settings.remove(KEY_REFRESH_TOKEN)
        current.value = readAccount()
    }

    private suspend fun readAccount(): SavedAccount? = settings.get(KEY_EMAIL)?.let(::SavedAccount)
}
