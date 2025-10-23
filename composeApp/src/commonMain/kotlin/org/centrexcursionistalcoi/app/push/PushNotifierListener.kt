package org.centrexcursionistalcoi.app.push

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.storage.settings

object PushNotifierListener : NotifierManager.Listener {
    override fun onNewToken(token: String) {
        Napier.i("onNewToken: $token")

        val oldTokenId = settings.getStringOrNull("fcm_token_id")

        CoroutineScope(defaultAsyncDispatcher).launch {
            if (oldTokenId != null) {
                val tokenRevoked = FCMTokenManager.revokeToken(oldTokenId)
                if (tokenRevoked) {
                    settings.remove("fcm_token_id")
                }
            }

            val newTokenId = FCMTokenManager.registerNewToken(token)
            if (newTokenId != null) {
                settings.putString("fcm_token", newTokenId)
            }
        }
    }

    override fun onPayloadData(data: PayloadData) {
        Napier.d { "Received push notification: $data" }
    }
}
