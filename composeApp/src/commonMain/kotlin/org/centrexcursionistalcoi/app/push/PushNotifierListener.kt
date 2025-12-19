package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.sync.*

object PushNotifierListener : NotifierManager.Listener {
    private val log = logging()

    override fun onNewToken(token: String) {
        log.i { "onNewToken: $token" }

        CoroutineScope(defaultAsyncDispatcher).launch {
            FCMTokenManager.renovate(token)
        }
    }

    override fun onPayloadData(data: PayloadData) {
        log.d { "Received push notification: $data" }

        try {
            val notification = PushNotification.fromData(data)
            when (notification) {
                is PushNotification.LendingUpdated -> {
                    log.d { "Received lending update notification for lending ID: ${notification.lendingId}" }
                    SyncLendingBackgroundJobLogic.scheduleAsync(
                        lendingId = notification.lendingId,
                        isRemoval = false,
                    )
                }

                is PushNotification.NewPost -> {
                    log.d { "Received new post notification. ID: ${notification.postId}" }
                    BackgroundJobCoordinator.scheduleAsync<SyncPostBackgroundJobLogic, SyncPostBackgroundJob>(
                        input = mapOf(
                            SyncPostBackgroundJobLogic.EXTRA_POST_ID to notification.postId.toString(),
                        ),
                        logic = SyncPostBackgroundJobLogic,
                    )
                }

                is PushNotification.NewEvent, is PushNotification.EventCancelled, is PushNotification.EventAssistanceUpdated -> {
                    log.d { "Received an event notification. ID: ${notification.eventId}" }
                    BackgroundJobCoordinator.scheduleAsync<SyncEventBackgroundJobLogic, SyncEventBackgroundJob>(
                        input = mapOf(
                            SyncEventBackgroundJobLogic.EXTRA_EVENT_ID to notification.eventId.toString(),
                        ),
                        logic = SyncEventBackgroundJobLogic,
                    )
                }

                is PushNotification.DepartmentJoinRequestUpdated -> {
                    log.d { "Received department join request update notification for request ID: ${notification.requestId}" }
                    BackgroundJobCoordinator.scheduleAsync<SyncDepartmentBackgroundJobLogic, SyncDepartmentBackgroundJob>(
                        input = mapOf(
                            SyncDepartmentBackgroundJobLogic.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                        ),
                        logic = SyncDepartmentBackgroundJobLogic,
                    )
                }

                is PushNotification.DepartmentKicked -> {
                    log.d { "Received department kicked notification for department ID: ${notification.departmentId}" }
                    BackgroundJobCoordinator.scheduleAsync<SyncDepartmentBackgroundJobLogic, SyncDepartmentBackgroundJob>(
                        input = mapOf(
                            SyncDepartmentBackgroundJobLogic.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                        ),
                        logic = SyncDepartmentBackgroundJobLogic,
                    )
                }
            }

            LocalNotifications.showPushNotification(notification, data)
        } catch (e: IllegalArgumentException) {
            log.e(e) { "Failed to parse push notification content" }
        }
    }
}
