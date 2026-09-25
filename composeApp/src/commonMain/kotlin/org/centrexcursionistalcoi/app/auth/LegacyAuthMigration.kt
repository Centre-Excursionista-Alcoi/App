package org.centrexcursionistalcoi.app.auth

import com.diamondedge.logging.logging
import com.russhwolf.settings.Settings
import org.koin.core.annotation.Singleton
import org.centrexcursionistalcoi.app.storage.settings as appSettings

/**
 * One-time migration of an account logged in before token authentication, run on the first launch after the
 * update: versions before it kept a session cookie (in the settings) and, on Android and iOS, the account's
 * password (in [CredentialsStore]), and the server no longer accepts cookies.
 *
 * The saved password is used to get a session, then every trace of the old authentication is removed. If there's
 * no password, or it doesn't work (wrong, or no connectivity), the account is logged out and the user has to log
 * in again.
 *
 * Delete this once no installed version can predate token authentication.
 */
@Singleton
class LegacyAuthMigration(
    private val credentialsStore: CredentialsStore,
    private val authBackend: AuthBackend,
) {
    private val log = logging()

    /** Where the old cookies are, replaceable by tests. */
    internal var settings: Settings = appSettings

    /** Whether versions before token authentication left anything behind. */
    private fun hasLegacyData(): Boolean =
        credentialsStore.getLegacyCredentials() != null || settings.keys.any { it.startsWith(LEGACY_COOKIE_PREFIX) }

    suspend fun run() {
        if (!hasLegacyData()) return

        val legacy = credentialsStore.getLegacyCredentials()
        val migrated = legacy != null && try {
            // Also deletes the saved password, see CredentialsStore.saveSession.
            authBackend.authenticate(legacy.email, legacy.password.concatToString())
            true
        } catch (e: Exception) {
            log.w(e) { "Could not log in with the saved password." }
            false
        }

        if (migrated) {
            removeLegacyCookies()
            log.i { "Migrated the saved account to token authentication." }
        } else {
            // Clears the saved password and the cookies along with everything else.
            log.i { "Could not migrate the saved account to token authentication, logging out." }
            authBackend.clearLocalData()
        }
    }

    private fun removeLegacyCookies() {
        settings.keys.filter { it.startsWith(LEGACY_COOKIE_PREFIX) }.forEach(settings::remove)
    }

    private companion object {
        /** Settings keys of the cookies stored by versions before token authentication. */
        const val LEGACY_COOKIE_PREFIX = "cookie."
    }
}
