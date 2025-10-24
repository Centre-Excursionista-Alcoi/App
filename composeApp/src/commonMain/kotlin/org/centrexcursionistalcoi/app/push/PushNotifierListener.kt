package org.centrexcursionistalcoi.app.push

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic

object PushNotifierListener : NotifierManager.Listener {
    override fun onNewToken(token: String) {
        Napier.i("onNewToken: $token")

        CoroutineScope(defaultAsyncDispatcher).launch {
            FCMTokenManager.renovate(token)
        }
    }

    override fun onPayloadData(data: PayloadData) {
        Napier.d { "Received push notification: $data" }

        try {
            val notification = PushNotification.fromData(data.mapValues { it.toString() })
            when (notification) {
                is PushNotification.LendingConfirmed -> {
                    // TODO: Show notification
                    BackgroundJobCoordinator.schedule<SyncLendingBackgroundJob>(
                        input = mapOf(SyncLendingBackgroundJobLogic.EXTRA_LENDING_ID to notification.lendingId.toString()),
                    )
                }
                else -> {
                    Napier.w { "Received an unhandled notification: ${notification.type}" }
                }
            }
        } catch (e: IllegalArgumentException) {
            Napier.e(e) { "Failed to parse push notification content" }
        }
    }
}
