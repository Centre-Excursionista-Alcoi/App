package org.centrexcursionistalcoi.app.push

import com.mmk.kmpnotifier.notification.NotifierManager
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.storage.settings

object FCMTokenManager {
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
        if (oldToken != null) {
            if (oldToken == newToken) {
                Napier.d { "Won't renovate token, already registered: $oldToken" }
                return
            }
            try {
                FCMTokenRemote.revokeToken(oldToken)
                settings.remove("fcm_token")
            } catch (e: ServerException) {
                Napier.e(e) { "Could not revoke old FCM token. New token won't be registered." }
                return
            }
        }

        try {
            FCMTokenRemote.registerNewToken(newToken)
            settings.putString("fcm_token", newToken)
        } catch (e: ServerException) {
            Napier.e(e) { "Could not register FCM token." }
        }
    }
}
