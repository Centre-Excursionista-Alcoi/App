package org.centrexcursionistalcoi.app.push

import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.serialization.kotlinx.json.json
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
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncLendingBackgroundJobLogic

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
                client.sse("/events") {
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
    }

    fun stopListening() {
        job?.cancel()
        job = null
        _isConnected.value = false
    }
}
