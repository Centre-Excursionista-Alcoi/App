package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.storage.settings

object FCMTokenManager {
    private val log = logging()

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
        val oldToken = settings.getStringOrNull("fcm_token")
        if (oldToken == newToken) {
            log.d { "Won't renovate token, already registered: $oldToken" }
            return
        }
        revoke()

        try {
            FCMTokenRemote.registerNewToken(newToken)
            settings.putString("fcm_token", newToken)
        } catch (e: ServerException) {
            log.e(e) { "Could not register FCM token." }
        }
    }

    suspend fun revoke(): Boolean {
        val oldToken = settings.getStringOrNull("fcm_token")
        return if (oldToken != null) {
            revoke(oldToken)
        } else {
            true
        }
    }

    suspend fun revoke(token: String): Boolean {
        return try {
            FCMTokenRemote.revokeToken(token)
            settings.remove("fcm_token")
            true
        } catch (e: ServerException) {
            log.e(e) { "Could not revoke FCM token." }
            false
        }
    }
}
