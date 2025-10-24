package org.centrexcursionistalcoi.app.push

import cea_app.composeapp.generated.resources.*
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.aakira.napier.Napier
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

object PushNotifierListener : NotifierManager.Listener {
    private fun showNotification(notificationTitleRes: StringResource, notificationBodyResource: StringResource, data: Map<String, String>) {
        CoroutineScope(defaultAsyncDispatcher).launch {
            val notifier = NotifierManager.getLocalNotifier()

            val notificationTitle = getString(notificationTitleRes)
            val notificationBody = getString(notificationBodyResource)

            notifier.notify {
                id = Random.nextInt()
                title = notificationTitle
                body = notificationBody
                payloadData = data
            }
        }
    }

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
                    BackgroundJobCoordinator.schedule<SyncLendingBackgroundJob>(
                        input = mapOf(SyncLendingBackgroundJobLogic.EXTRA_LENDING_ID to notification.lendingId.toString()),
                    )

                    showNotification(
                        Res.string.notification_lending_confirmed_title,
                        Res.string.notification_lending_confirmed_message,
                        data.mapValues { it.toString() }
                    )
                }

                // --- Admin notifications ---
                is PushNotification.NewLendingRequest -> {
                    // TODO: Show notification
                    BackgroundJobCoordinator.schedule<SyncLendingBackgroundJob>(
                        input = mapOf(SyncLendingBackgroundJobLogic.EXTRA_LENDING_ID to notification.lendingId.toString()),
                    )
                }
                is PushNotification.NewMemoryUpload -> {
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
