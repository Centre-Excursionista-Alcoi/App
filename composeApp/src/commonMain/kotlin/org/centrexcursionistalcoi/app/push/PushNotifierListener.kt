package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.push.PushListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncDepartmentBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncEntityBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncEventBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob.Companion.EXTRA_IS_REMOVAL
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob.Companion.EXTRA_LENDING_ID
import org.koin.core.annotation.Singleton

@Singleton
class PushNotifierListener(
    private val dispatcherProvider: DispatcherProvider,
    private val coordinator: BackgroundJobCoordinator,
    private val fcmTokenManager: FCMTokenManager,
    private val profileRepository: ProfileRepository,
    private val localNotifications: LocalNotifications,
) : PushListener {
    private val log = logging()

    override fun onNewToken(token: String) {
        log.i { "onNewToken: $token" }

        val isLoggedIn = runBlocking { profileRepository.isLoggedIn() }
        if (!isLoggedIn) {
            log.i { "User is not logged in, skipping token registration." }
            return
        }

        CoroutineScope(dispatcherProvider.io + GlobalAsyncErrorHandler.coroutineExceptionHandler).launch {
            fcmTokenManager.renovate(token)
        }
    }

    override fun onPayloadData(data: PayloadData) {
        log.d { "Received push notification: $data" }

        try {
            val notification = PushNotification.fromData(data)
            when (notification) {
                is PushNotification.LendingUpdated -> {
                    log.d { "Received lending update notification for lending ID: ${notification.lendingId}" }
                    coordinator.scheduleAsync<SyncLendingBackgroundJob>(
                        name = SyncLendingBackgroundJob.NAME,
                        input = mapOf(
                            EXTRA_LENDING_ID to notification.lendingId.toString(),
                            EXTRA_IS_REMOVAL to SyncLendingBackgroundJob.isRemoval(notification).toString(),
                        ),
                    )
                }

                is PushNotification.EventAssistanceUpdated -> {
                    log.d { "Received an event notification. ID: ${notification.eventId}" }
                    coordinator.scheduleAsync<SyncEventBackgroundJob>(
                        name = SyncEventBackgroundJob.NAME,
                        input = mapOf(
                            SyncEventBackgroundJob.EXTRA_EVENT_ID to notification.eventId.toString(),
                        ),
                    )
                }

                is PushNotification.DepartmentJoinRequestUpdated -> {
                    log.d { "Received department join request update notification for request ID: ${notification.requestId}" }
                    coordinator.scheduleAsync<SyncDepartmentBackgroundJob>(
                        name = SyncDepartmentBackgroundJob.NAME,
                        input = mapOf(
                            SyncDepartmentBackgroundJob.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                        ),
                    )
                }

                is PushNotification.DepartmentKicked -> {
                    log.d { "Received department kicked notification for department ID: ${notification.departmentId}" }
                    coordinator.scheduleAsync<SyncDepartmentBackgroundJob>(
                        name = SyncDepartmentBackgroundJob.NAME,
                        input = mapOf(
                            SyncDepartmentBackgroundJob.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                        ),
                    )
                }

                is PushNotification.EntityUpdated -> {
                    log.d { "Received entity updated notification for ${notification.entityClass}#${notification.entityId}" }
                    coordinator.scheduleAsync<SyncEntityBackgroundJob>(
                        name = SyncEntityBackgroundJob.NAME,
                        input = mapOf(
                            SyncEntityBackgroundJob.EXTRA_ENTITY_CLASS to notification.entityClass,
                            SyncEntityBackgroundJob.EXTRA_ENTITY_ID to notification.entityId,
                        ),
                    )
                }
                is PushNotification.NewSpaceLending -> {
                    // Only shown: spaces are not synced by the app yet
                    log.d { "Received a new space lending notification: ${notification.spaceLendingId}" }
                }
                is PushNotification.EntityDeleted -> {
                    log.d { "Received entity deleted notification for ${notification.entityClass}#${notification.entityId}" }
                    coordinator.scheduleAsync<SyncEntityBackgroundJob>(
                        name = SyncEntityBackgroundJob.NAME,
                        input = mapOf(
                            SyncEntityBackgroundJob.EXTRA_ENTITY_CLASS to notification.entityClass,
                            SyncEntityBackgroundJob.EXTRA_ENTITY_ID to notification.entityId,
                            SyncEntityBackgroundJob.EXTRA_IS_DELETE to "true",
                        ),
                    )
                }
            }

            runBlocking {
                localNotifications.showPushNotification(notification, data)
            }
        } catch (e: IllegalArgumentException) {
            log.e(e) { "Failed to parse push notification content" }
        }
    }
}
