package org.centrexcursionistalcoi.app.push

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private var job: Job? = null

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
        if (!PlatformSSEConfiguration.enableSSE) {
            Napier.w(tag = "SSE") { "SSE is disabled on this platform." }
            return
        }
        if (job != null) {
            Napier.d(tag = "SSE") { "SSE already configured." }
            return
        }

        Napier.d { "Setting up SSE..." }
        job = CoroutineScope(defaultAsyncDispatcher).launch {
            client.sse("/events") {
                Napier.i(tag = "SSE") { "Listening for events." }
                while (true) {
                    incoming.collect { event ->
                        val type = event.event
                        val data = event.data?.let { data ->
                            data.split('&').map { it.substringBefore('=') to it.substringAfter('=') }
                        }?.toMap() ?: emptyMap()
                        Napier.d(tag = "SSE") { "Received SSE notification. Type=$type, Data=$data" }
                        try {
                            val notification = PushNotification.fromData(
                                data + ("type" to type)
                            )
                            Napier.d(tag = "SSE") { "Notification: $notification" }

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
                            Napier.e(e, tag = "SSE") { "Received an invalid SSE notification." }
                        }
                    }
                }
            }
        }
    }

    fun stopListening() {
        job?.cancel()
        job = null
    }
}
