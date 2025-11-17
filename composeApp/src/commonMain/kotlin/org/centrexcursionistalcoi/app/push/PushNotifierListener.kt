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
            val notification = PushNotification.fromData(data)
            if (notification is PushNotification.LendingUpdated) {
                Napier.d { "Received lending update notification for lending ID: ${notification.lendingId}" }
                BackgroundJobCoordinator.scheduleAsync<SyncLendingBackgroundJobLogic, SyncLendingBackgroundJob>(
                    input = mapOf(
                        SyncLendingBackgroundJobLogic.EXTRA_LENDING_ID to notification.lendingId.toString(),
                        SyncLendingBackgroundJobLogic.EXTRA_IS_REMOVAL to (notification is PushNotification.LendingCancelled).toString(),
                    ),
                    logic = SyncLendingBackgroundJobLogic,
                )
            }

            LocalNotifications.showPushNotification(notification, data)
        } catch (e: IllegalArgumentException) {
            Napier.e(e) { "Failed to parse push notification content" }
        }
    }
}
