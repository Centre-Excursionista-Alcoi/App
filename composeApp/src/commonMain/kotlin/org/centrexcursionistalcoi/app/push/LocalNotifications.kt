package org.centrexcursionistalcoi.app.push

import cea_app.composeapp.generated.resources.*
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

object LocalNotifications {
    private val log = logging()

    fun showNotification(notificationTitle: String, notificationBody: String, data: Map<String, *>) {
        CoroutineScope(defaultAsyncDispatcher).launch {
            val notifier = NotifierManager.getLocalNotifier()

            notifier.notify {
                id = Random.nextInt()
                title = notificationTitle
                body = notificationBody
                payloadData = data.mapValues { (_, value) ->
                    when (value) {
                        is String -> value
                        is Number -> value.toString()
                        is Boolean -> value.toString()
                        else -> value.toString()
                    }
                }
            }
        }
    }

    fun showNotification(notificationTitleRes: StringResource, notificationBodyRes: StringResource, data: Map<String, *>) {
        CoroutineScope(defaultAsyncDispatcher).launch {
            val notifier = NotifierManager.getLocalNotifier()

            val notificationTitle = getString(notificationTitleRes)
            val notificationBody = getString(notificationBodyRes)

            notifier.notify {
                id = Random.nextInt()
                title = notificationTitle
                body = notificationBody
                payloadData = data.mapValues { (_, value) ->
                    when (value) {
                        is String -> value
                        is Number -> value.toString()
                        is Boolean -> value.toString()
                        else -> value.toString()
                    }
                }
            }
        }
    }

    fun showPushNotification(notification: PushNotification, data: Map<String, *>) {
        val profile = ProfileRepository.getProfile()

        when (notification) {
            is PushNotification.LendingConfirmed -> {
                // Only show if the notification is for the current user
                if (!notification.isSelf) {
                    log.d { "Ignoring lending confirmed notification for another user: ${notification.userSub}" }
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
                if (!notification.isSelf) {
                    log.d { "Ignoring lending cancelled notification for another user: ${notification.userSub}" }
                    return
                }

                showNotification(
                    Res.string.notification_lending_cancelled_title,
                    Res.string.notification_lending_cancelled_message,
                    data
                )
            }
            is PushNotification.LendingTaken -> {
                if (!notification.isSelf) {
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
                if (!notification.isSelf) {
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
            is PushNotification.LendingPartiallyReturned -> {
                if (!notification.isSelf) {
                    // The lending is for the current user
                    showNotification(
                        Res.string.notification_lending_returned_partial_title,
                        Res.string.notification_lending_returned_partial_message,
                        data
                    )
                }
            }

            is PushNotification.DepartmentJoinRequestUpdated -> {
                // Only show if the notification is for the current user
                if (!notification.isSelf) {
                    log.d { "Ignoring join updated notification for another user: ${notification.userSub}" }
                    return
                }

                if (notification.isConfirmed) {
                    showNotification(
                        Res.string.notification_join_request_approved_title,
                        Res.string.notification_join_request_approved_message,
                        data
                    )
                } else {
                    showNotification(
                        Res.string.notification_join_request_denied_title,
                        Res.string.notification_join_request_denied_message,
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
                log.w { "Received an unhandled notification: ${notification.type}" }
            }
        }
    }
}
