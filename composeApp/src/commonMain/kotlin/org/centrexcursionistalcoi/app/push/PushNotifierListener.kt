package org.centrexcursionistalcoi.app.push

import cea_app.composeapp.generated.resources.*
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.github.aakira.napier.Napier
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

object PushNotifierListener : NotifierManager.Listener {
    private fun showNotification(notificationTitleRes: StringResource, notificationBodyResource: StringResource, data: Map<String, *>) {
        CoroutineScope(defaultAsyncDispatcher).launch {
            val notifier = NotifierManager.getLocalNotifier()

            val notificationTitle = getString(notificationTitleRes)
            val notificationBody = getString(notificationBodyResource)

            notifier.notify {
                id = Random.nextInt()
                title = notificationTitle
                body = notificationBody
                // TODO - payloadData = data
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
            val notification = PushNotification.fromData(data)
            if (notification is PushNotification.LendingUpdated) {
                Napier.d { "Received lending update notification for lending ID: ${notification.lendingId}" }
                BackgroundJobCoordinator.schedule<SyncLendingBackgroundJob>(
                    input = mapOf(SyncLendingBackgroundJobLogic.EXTRA_LENDING_ID to notification.lendingId.toString()),
                )
            }

            val profile = ProfileRepository.getProfile()

            when (notification) {
                is PushNotification.LendingConfirmed -> {
                    // Only show if the notification is for the current user
                    if (profile?.sub != notification.userSub) {
                        Napier.d { "Ignoring lending confirmed notification for another user: ${notification.userSub}" }
                        return
                    }

                    showNotification(
                        Res.string.notification_lending_confirmed_title,
                        Res.string.notification_lending_confirmed_message,
                        data
                    )
                }
                is PushNotification.LendingCancelled -> {
                    // Only show if the notification is for the current user
                    if (profile?.sub != notification.userSub) {
                        Napier.d { "Ignoring lending cancelled notification for another user: ${notification.userSub}" }
                        return
                    }

                    showNotification(
                        Res.string.notification_lending_cancelled_title,
                        Res.string.notification_lending_cancelled_message,
                        data
                    )
                }
                is PushNotification.LendingTaken -> {
                    if (profile?.sub != notification.userSub) {
                        // The lending is for another user, the logged-in user is an admin
                        showNotification(
                            Res.string.notification_lending_given_title,
                            Res.string.notification_lending_given_message,
                            data
                        )
                    } else {
                        // The lending is for the current user
                        showNotification(
                            Res.string.notification_lending_taken_title,
                            Res.string.notification_lending_taken_message,
                            data
                        )
                    }
                }
                is PushNotification.LendingReturned -> {
                    if (profile?.sub != notification.userSub) {
                        // The lending is for another user, the logged-in user is an admin
                        showNotification(
                            Res.string.notification_lending_returned_other_title,
                            Res.string.notification_lending_returned_other_message,
                            data
                        )
                    } else {
                        // The lending is for the current user
                        showNotification(
                            Res.string.notification_lending_returned_title,
                            Res.string.notification_lending_returned_message,
                            data
                        )
                    }
                }

                // --- Admin notifications --
                is PushNotification.NewLendingRequest -> {
                    showNotification(
                        Res.string.notification_lending_created_title,
                        Res.string.notification_lending_created_message,
                        data
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
