package org.centrexcursionistalcoi.app.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.utils.Zero
import org.junit.jupiter.api.assertNotNull

class TestPush {
    @Test
    fun `test flow for admins`() = runTest {
        var notification: PushNotification? = null
        val adminSession = UserSession(sub = "abc", fullName = "Admin", email = "admin@example.com", groups = listOf(ADMIN_GROUP_NAME))

        val job = CoroutineScope(Job()).launch {
            Push.flow(adminSession).collect {
                println("Received notification: $it")
                notification = it
            }
        }

        try {
            // push notification includes admins, will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "def",
                includeAdmins = true,
            )

            notification.let { noti ->
                assertNotNull(noti)
                assertEquals(noti.type, "LendingConfirmed")
            }

            notification = null // reset for next test


            // push notification doesn't include admins, and sub doesn't match. Won't be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "def",
                includeAdmins = false,
            )

            assertNull(notification)

            notification = null // reset for next test


            // push notification doesn't include admins, and sub matches. Will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "abc",
                includeAdmins = false,
            )

            notification.let { noti ->
                assertNotNull(noti)
                assertEquals(noti.type, "LendingConfirmed")
            }
        } finally {
            job.cancel()
        }
    }
}
