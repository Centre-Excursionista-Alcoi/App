package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.module
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.ClientInfo
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.test.*
import org.centrexcursionistalcoi.app.utils.Zero

/**
 * Runs against a real Netty server and client, not the in-memory test host: since Ktor 3.6.0 the test host never
 * delivers an SSE response to the client (a bare `testApplication` + `client.sse()` times out, see #660). Remove this
 * once that's fixed upstream and use `runApplicationTest`'s `client` again.
 */
class TestSSE : ApplicationTestBase() {
    @Test
    fun `test events - admin`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        disablePush = false,
    ) {
        val server = embeddedServer(Netty, port = 0) { module(isTesting = true) }.start(wait = false)
        val accessToken = Database {
            AuthTokens.startSession(FakeAdminUser.provideEntity(), AuthSessionMethod.PASSWORD, ClientInfo(null, "test"))
        }.accessToken
        val port = server.engine.resolvedConnectors().first().port
        val client = HttpClient(Java) { install(SSE) }
        var connectionEstablished = false
        var type: String? = null
        var data: Map<String, String>? = null

        val job = CoroutineScope(Job()).launch {
            client.sse("http://127.0.0.1:$port/sse", request = { header(HttpHeaders.Authorization, "Bearer $accessToken") }) {
                connectionEstablished = true
                println("Connection Established!")
                while (true) {
                    incoming.collect { event ->
                        println("Received event: ${event.event}, data: ${event.data}")
                        type = event.event
                        data = event.data?.let { data ->
                            data.split('&').map { it.substringBefore('=') to it.substringAfter('=') }
                        }?.toMap()
                    }
                }
            }
        }

        try {
            // wait for connection to be established
            withTimeout(10.seconds) {
                while (!connectionEstablished) delay(10)
            }
            delay(50) // wait for the event to be received
            assertEquals("connection", type, "Didn't receive the connection event.") // initial connection event
            type = null // reset for next tests


            // push notification includes admins, will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "other",
                includeAdmins = true,
            )
            delay(50) // wait for the event to be received

            type.let { notificationType ->
                assertNotNull(notificationType)
                assertEquals("LendingConfirmed", notificationType)
            }
            data.let { notificationData ->
                assertNotNull(notificationData)
                assertEquals(Uuid.Zero.toString(), notificationData["lendingId"])
                assertEquals("xyz", notificationData["userSub"])
            }

            // reset for next test
            type = null
            data = null


            // push notification doesn't include admins, and sub doesn't match. Won't be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "other",
                includeAdmins = false,
            )
            delay(50) // wait for the event to be received

            assertEquals(null, type)
            assertEquals(null, data)


            // push notification doesn't include admins, and sub matches. Will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "test-user-id-456",
                includeAdmins = false,
            )
            delay(50) // wait for the event to be received

            type.let { notificationType ->
                assertNotNull(notificationType)
                assertEquals("LendingConfirmed", notificationType)
            }
            data.let { notificationData ->
                assertNotNull(notificationData)
                assertEquals(Uuid.Zero.toString(), notificationData["lendingId"])
                assertEquals("xyz", notificationData["userSub"])
            }
        } catch(_: TimeoutCancellationException) {
            throw AssertionError("Connection was not established in time.")
        } finally {
            job.cancel()
            client.close()
            server.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
        }
    }
}
