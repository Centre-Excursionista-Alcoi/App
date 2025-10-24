package org.centrexcursionistalcoi.app.push

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher

object PushNotifierListener : NotifierManager.Listener {
    override fun onNewToken(token: String) {
        Napier.i("onNewToken: $token")

        CoroutineScope(defaultAsyncDispatcher).launch {
            FCMTokenManager.renovate(token)
        }
    }

    override fun onPayloadData(data: PayloadData) {
        Napier.d { "Received push notification: $data" }
    }
}
