package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.storage.settings

@OptIn(ExperimentalSettingsApi::class)
object FCMTokenManager {
    private val log = logging()

    private const val SETTINGS_FCM_TOKEN = "fcm_token"

    val tokenFlow get() = settings.getStringOrNullFlow(SETTINGS_FCM_TOKEN)

    /**
     * Renovate the FCM token if needed.
     * The token is obtained from the [NotifierManager].
     */
    suspend fun renovate() {
        val token = NotifierManager.getPushNotifier().getToken()
        if (token != null) {
            renovate(token)
        }
    }

    /**
     * Renovate the FCM token if needed.
     * @param newToken The new FCM token to register.
     */
    suspend fun renovate(newToken: String) {
        val oldToken = settings.getStringOrNull(SETTINGS_FCM_TOKEN)
        if (oldToken == newToken) {
            log.d { "Won't renovate token, already registered: $oldToken" }
            return
        }
        revoke()

        try {
            FCMTokenRemote.registerNewToken(newToken)
            settings.putString(SETTINGS_FCM_TOKEN, newToken)
        } catch (e: ServerException) {
            log.e(e) { "Could not register FCM token." }
        }
    }

    suspend fun revoke(): Boolean {
        val oldToken = settings.getStringOrNull(SETTINGS_FCM_TOKEN)
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
        } catch (e: ServerException) {
            log.e(e) { "Could not revoke FCM token." }
            false
        }
    }
}
