package org.centrexcursionistalcoi.app.push

import androidx.datastore.preferences.core.stringPreferencesKey
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.push.firebase.firebasePushNotifier
import kotlinx.coroutines.CancellationException
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.annotation.Singleton

@Singleton
class FCMTokenManager(
    private val settings: SettingsStore
) {
    companion object {
        private val SETTINGS_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }

    private val log = logging()

    val tokenFlow get() = settings.getFlow(SETTINGS_FCM_TOKEN)

    /**
     * Renovate the FCM token if needed.
     * The token is obtained from the [KMPNotifier].
     */
    suspend fun renovate() {
        val token = KMPNotifier.firebasePushNotifier.getToken()
        if (token != null) {
            renovate(token)
        }
    }

    /**
     * Renovate the FCM token if needed.
     * @param newToken The new FCM token to register.
     */
    suspend fun renovate(newToken: String) {
        val oldToken = settings.get(SETTINGS_FCM_TOKEN)
        if (oldToken == newToken) {
            log.d { "Won't renovate token, already registered: $oldToken" }
            return
        }
        revoke()

        try {
            FCMTokenRemote.registerNewToken(newToken)
            settings.set(SETTINGS_FCM_TOKEN, newToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Registered again with the next token, or the next login
            log.w(e) { "Could not register FCM token." }
        }
    }

    suspend fun revoke(): Boolean {
        val oldToken = settings.get(SETTINGS_FCM_TOKEN)
        return if (oldToken != null) {
            revoke(oldToken)
        } else {
            true
        }
    }

    suspend fun revoke(token: String): Boolean {
        return try {
            FCMTokenRemote.revokeToken(token)
            settings.remove(SETTINGS_FCM_TOKEN)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best-effort, e.g. no connectivity while logging out: the token stops being used anyway once the
            // user is logged out.
            log.w(e) { "Could not revoke FCM token." }
            false
        }
    }
}
