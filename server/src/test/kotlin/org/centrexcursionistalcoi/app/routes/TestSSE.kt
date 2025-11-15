package org.centrexcursionistalcoi.app.routes

import io.ktor.client.plugins.sse.sse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.test.*
import org.centrexcursionistalcoi.app.utils.Zero
import org.junit.jupiter.api.assertNotNull

class TestSSE : ApplicationTestBase() {
    @Test
    fun `test events - admin`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
    ) {
        var connectionEstablished = false
        var type: String? = null
        var data: Map<String, String>? = null

        val job = CoroutineScope(Job()).launch {
            client.sse("/events") {
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
            while (!connectionEstablished) delay(10)
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
        } finally {
            job.cancel()
        }
    }
}
