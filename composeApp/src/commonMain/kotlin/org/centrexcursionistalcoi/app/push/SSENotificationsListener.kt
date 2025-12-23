package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.network.configureLogging
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage
import org.centrexcursionistalcoi.app.sync.*

object SSENotificationsListener {
    private val log = logging()

    private var job: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _sseException = MutableStateFlow<SSEClientException?>(null)
    val sseException = _sseException.asStateFlow()

    private val client = HttpClient {
        defaultRequest {
            url(BuildKonfig.SERVER_URL)
        }
        install(HttpCookies) {
            storage = SettingsCookiesStorage.Default
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(SSE)
        configureLogging()
    }

    fun startListening() {
        _sseException.value = null

        if (!PlatformSSEConfiguration.enableSSE) {
            log.w(tag = "SSE") { "SSE is disabled on this platform." }
            return
        }
        if (job != null) {
            log.d(tag = "SSE") { "SSE already configured." }
            return
        }

        log.d { "Setting up SSE..." }
        job = CoroutineScope(defaultAsyncDispatcher).launch {
            try {
                client.sse("/sse") {
                    log.i(tag = "SSE") { "Listening for events." }
                    _isConnected.value = true
                    while (true) {
                        incoming.collect { event ->
                            val type = event.event
                            val data = event.data?.let { data ->
                                data.split('&').map { it.substringBefore('=') to it.substringAfter('=') }
                            }?.toMap() ?: emptyMap()

                            if (type == "connection") {
                                log.d(tag = "SSE") { "Received connection event." }
                                return@collect
                            }

                            log.d(tag = "SSE") { "Received SSE notification. Type=$type, Data=$data" }
                            try {
                                val notification = PushNotification.fromData(
                                    data + ("type" to type)
                                )
                                log.d(tag = "SSE") { "Notification: $notification" }

                                if (notification is PushNotification.LendingUpdated) {
                                    log.d { "Received lending update notification for lending ID: ${notification.lendingId}" }
                                    BackgroundJobCoordinator.scheduleAsync<SyncLendingBackgroundJobLogic, SyncLendingBackgroundJob>(
                                        input = mapOf(
                                            SyncLendingBackgroundJobLogic.EXTRA_LENDING_ID to notification.lendingId.toString(),
                                            SyncLendingBackgroundJobLogic.EXTRA_IS_REMOVAL to (notification is PushNotification.LendingCancelled).toString(),
                                        ),
                                        logic = SyncLendingBackgroundJobLogic,
                                    )
                                } else if (notification is PushNotification.DepartmentJoinRequestUpdated) {
                                    log.d { "Received department update notification for department ID: ${notification.departmentId}" }
                                    BackgroundJobCoordinator.scheduleAsync<SyncDepartmentBackgroundJobLogic, SyncDepartmentBackgroundJob>(
                                        input = mapOf(
                                            SyncDepartmentBackgroundJobLogic.EXTRA_DEPARTMENT_ID to notification.departmentId.toString(),
                                        ),
                                        logic = SyncDepartmentBackgroundJobLogic,
                                    )
                                } else if (notification is PushNotification.EntityUpdated) {
                                    log.d { "Received ${notification.entityClass} update notification for ID: ${notification.entityId}" }
                                    BackgroundJobCoordinator.scheduleAsync<SyncEntityBackgroundJobLogic, SyncEntityBackgroundJob>(
                                        input = mapOf(
                                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_CLASS to notification.entityClass,
                                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_ID to notification.entityId,
                                        ),
                                        logic = SyncEntityBackgroundJobLogic,
                                    )
                                } else if (notification is PushNotification.EntityDeleted) {
                                    log.d { "Received ${notification.entityClass} delete notification for ID: ${notification.entityId}" }
                                    BackgroundJobCoordinator.scheduleAsync<SyncEntityBackgroundJobLogic, SyncEntityBackgroundJob>(
                                        input = mapOf(
                                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_CLASS to notification.entityClass,
                                            SyncEntityBackgroundJobLogic.EXTRA_ENTITY_ID to notification.entityId,
                                            SyncEntityBackgroundJobLogic.EXTRA_IS_DELETE to "true",
                                        ),
                                        logic = SyncEntityBackgroundJobLogic,
                                    )
                                }

                                LocalNotifications.showPushNotification(notification, data)
                            } catch (e: IllegalArgumentException) {
                                log.e(e, tag = "SSE") { "Received an invalid SSE notification." }
                            }
                        }
                    }
                }
                _isConnected.value = false
            } catch (e: SSEClientException) {
                log.e(e, tag = "SSE") { "Connection failed." }
                _sseException.value = e
            }
        }
        job?.invokeOnCompletion { job = null }

        log.d { "SSE is running..." }
    }

    fun stopListening() {
        job?.cancel()
        job = null
        _isConnected.value = false
    }
}
