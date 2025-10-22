package org.centrexcursionistalcoi.app.push

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.aakira.napier.Napier

object PushNotifierListener : NotifierManager.Listener {
    override fun onNewToken(token: String) {
        Napier.i("onNewToken: $token")
    }

    override fun onPayloadData(data: PayloadData) {
        Napier.d { "Received push notification: $data" }
    }
}
