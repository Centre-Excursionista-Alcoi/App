package org.centrexcursionistalcoi.app.push

import cea_app.composeapp.generated.resources.*
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.push.PushNotification.TargetedNotification
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.random.Random

object LocalNotifications {
    private val log = logging()

    /**
     * Checks if the targeted notification is for the current user.
     *
     * Compares the [TargetedNotification.userSub] with the current profile's [ProfileResponse.sub].
     * @return true if the notification is for the current user, false otherwise.
     */
    fun TargetedNotification.checkIsSelf(): Boolean {
        val profile = ProfileRepository.getProfile() ?: return false
        return profile.sub == userSub
    }

    private fun notify(notificationTitle: String, notificationBody: String, data: Map<String, *>) {
        NotifierManager.getLocalNotifier().notify {
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

    fun showNotification(notificationTitle: String, notificationBody: String, data: Map<String, *>) {
        CoroutineScope(defaultAsyncDispatcher).launch {
            notify(notificationTitle, notificationBody, data)
        }
    }

    fun showNotification(notificationTitleRes: StringResource, notificationBodyRes: StringResource, data: Map<String, *>) {
        CoroutineScope(defaultAsyncDispatcher).launch {
            notify(
                notificationTitle = getString(notificationTitleRes),
                notificationBody = getString(notificationBodyRes),
                data
            )
        }
    }

    fun showPushNotification(notification: PushNotification, data: Map<String, *>) {
        when (notification) {
            is PushNotification.LendingConfirmed -> {
                // Only show if the notification is for the current user
                if (!notification.checkIsSelf()) {
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
                if (!notification.checkIsSelf()) {
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
                if (!notification.checkIsSelf()) {
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
                if (!notification.checkIsSelf()) {
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
                if (!notification.checkIsSelf()) {
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
                if (!notification.checkIsSelf()) {
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
            is PushNotification.DepartmentKicked -> {
                // Only show if the notification is for the current user
                if (!notification.checkIsSelf()) {
                    log.d { "Ignoring department kicked notification for another user: ${notification.userSub}" }
                    return
                }

                runBlocking {
                    DepartmentsRepository.get(notification.departmentId)?.let { event ->
                        showNotification(
                            getString(Res.string.notification_department_kicked_title),
                            getString(
                                Res.string.notification_department_kicked_message,
                                event.displayName,
                            ),
                            data
                        )
                    }
                }
            }

            is PushNotification.EventCancelled -> {
                // Only show if the notification is for the current user
                if (!notification.checkIsSelf()) {
                    log.d { "Ignoring department kicked notification for another user: ${notification.userSub}" }
                    return
                }

                runBlocking {
                    EventsRepository.get(notification.eventId)?.let { event ->
                        showNotification(
                            getString(Res.string.notification_event_cancelled_title),
                            getString(
                                Res.string.notification_event_cancelled_message,
                                event.title,
                            ),
                            data
                        )
                    }
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
