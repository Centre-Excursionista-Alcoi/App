package org.centrexcursionistalcoi.app.auth

import androidx.datastore.preferences.core.longPreferencesKey
import com.diamondedge.logging.logging
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.SecurityRemoteRepository
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.annotation.Singleton

/**
 * Moves users who sign in with a password to passkeys, without ever forcing them:
 * - Right after a password login, the platform may upgrade the account to a passkey on its own
 *   ([afterPasswordLogin]).
 * - Otherwise, they're reminded, explaining what a passkey is ([shouldRemind]); "not now" waits [snoozeDuration]
 *   before asking again.
 */
@Singleton
class PasskeyUpgrade(
    private val passkeys: Passkeys,
    private val security: SecurityRemoteRepository,
    private val settings: SettingsStore,
    private val profileRepository: ProfileRepository,
    dispatcherProvider: DispatcherProvider,
) {
    private val log = logging()

    /** Outlives the screen that logged in: the platform may take a moment to decide. */
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private val snoozeDuration = 14.days

    /** Per user, and kept on logout (see [SettingsStore.clear]): someone else signing in here gets their own. */
    private fun snoozedUntilKey(sub: String) =
        longPreferencesKey("${SettingsStore.DEVICE_KEY_PREFIX}passkey_reminder_snoozed_until.$sub")

    /** Tries the platform's automatic passkey upgrade, in the background, if the account has no passkey yet. */
    fun afterPasswordLogin() {
        if (!passkeys.isSupported) return
        scope.launch {
            try {
                if (security.getSecurityInfo().passkeys.isNotEmpty()) return@launch
                val response = passkeys.createAutomatically(security.passkeyCreationOptions()) ?: return@launch
                security.addPasskey(response, passkeys.deviceName)
                log.i { "The account was upgraded to a passkey." }
            } catch (e: Exception) {
                // Not worth bothering the user: they'll be reminded instead.
                log.w(e) { "Could not upgrade the account to a passkey automatically." }
            }
        }
    }

    /** Whether to remind the logged-in user to create a passkey now. */
    suspend fun shouldRemind(): Boolean {
        if (!passkeys.isSupported) return false
        val sub = profileRepository.getProfile()?.sub ?: return false
        val snoozedUntil = settings.get(snoozedUntilKey(sub))
        if (snoozedUntil != null && Clock.System.now().toEpochMilliseconds() < snoozedUntil) return false
        return try {
            security.getSecurityInfo().passkeys.isEmpty()
        } catch (e: Exception) {
            log.w(e) { "Could not check whether the user has passkeys." }
            false
        }
    }

    /** The user chose "not now": waits [snoozeDuration] before asking again. */
    suspend fun snooze() {
        val sub = profileRepository.getProfile()?.sub ?: return
        settings.set(snoozedUntilKey(sub), (Clock.System.now() + snoozeDuration).toEpochMilliseconds())
    }
}
