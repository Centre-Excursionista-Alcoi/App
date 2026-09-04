package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncDepartmentBackgroundJobLogic
import org.centrexcursionistalcoi.app.sync.SyncEntityBackgroundJobLogic
import org.centrexcursionistalcoi.app.sync.SyncEventBackgroundJobLogic
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic.Companion.EXTRA_IS_REMOVAL
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic.Companion.EXTRA_LENDING_ID
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Singleton
class PushNotifierListener(
    private val coordinator: BackgroundJobCoordinator,
) : NotifierManager.Listener, KoinComponent {
    private val log = logging()

    override fun onNewToken(token: String) {
        log.i { "onNewToken: $token" }

        CoroutineScope(get<DispatcherProvider>().io).launch {
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
                    coordinator.scheduleAsync<SyncLendingBackgroundJobLogic>(
                        input = mapOf(
                            EXTRA_LENDING_ID to notification.lendingId.toString(),
                            EXTRA_IS_REMOVAL to false.toString(),
                        ),
                    )
                }

                is PushNotification.EventAssistanceUpdated -> {
                    log.d { "Received an event notification. ID: ${notification.eventId}" }
                    coordinator.scheduleAsync<SyncEventBackgroundJobLogic>(
                        input = mapOf(
                            SyncEventBackgroundJobLogic.EXTRA_EVENT_ID to notification.eventId.toString(),
                        ),
                    )
                }

                is PushNotification.DepartmentJoinRequestUpdated -> {
                    log.d { "Received department join request update notification for request ID: ${notification.requestId}" }
                    coordinator.scheduleAsync<SyncDepartmentBackgroundJobLogic>(
                        input = mapOf(
                            SyncDepartmentBackgroundJobLogic.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                        ),
                    )
                }

                is PushNotification.DepartmentKicked -> {
                    log.d { "Received department kicked notification for department ID: ${notification.departmentId}" }
                    coordinator.scheduleAsync<SyncDepartmentBackgroundJobLogic>(
                        input = mapOf(
                            SyncDepartmentBackgroundJobLogic.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                        ),
                    )
                }

                is PushNotification.EntityUpdated -> {
                    log.d { "Received entity updated notification for ${notification.entityClass}#${notification.entityId}" }
                    coordinator.scheduleAsync<SyncEntityBackgroundJobLogic>(
                        input = mapOf(
                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_CLASS to notification.entityClass,
                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_ID to notification.entityId,
                        ),
                    )
                }
                is PushNotification.EntityDeleted -> {
                    log.d { "Received entity deleted notification for ${notification.entityClass}#${notification.entityId}" }
                    coordinator.scheduleAsync<SyncEntityBackgroundJobLogic>(
                        input = mapOf(
                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_CLASS to notification.entityClass,
                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_ID to notification.entityId,
                            SyncEntityBackgroundJobLogic.EXTRA_IS_DELETE to "true",
                        ),
                    )
                }
            }

            LocalNotifications.showPushNotification(notification, data)
        } catch (e: IllegalArgumentException) {
            log.e(e) { "Failed to parse push notification content" }
        }
    }
}
