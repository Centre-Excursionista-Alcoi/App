package org.centrexcursionistalcoi.app.notifications

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.utils.Zero
import org.junit.jupiter.api.assertNotNull

class TestPush {
    @Test
    fun `test flow for admins`() = runTest {
        val adminSession = UserSession(sub = "abc", fullName = "Admin", email = "admin@example.com", groups = listOf(ADMIN_GROUP_NAME))
        Push.flow(adminSession).test {
            // push notification includes admins, will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz", true),
                userSub = "def",
                includeAdmins = true,
            )

            awaitItem().let { noti ->
                assertNotNull(noti)
                assertEquals(noti.type, "LendingConfirmed")
            }


            // push notification doesn't include admins, and sub doesn't match. Won't be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz", true),
                userSub = "def",
                includeAdmins = false,
            )

            // should not emit anything
            expectNoEvents()


            // push notification doesn't include admins, and sub matches. Will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz", true),
                userSub = "abc",
                includeAdmins = false,
            )

            awaitItem().let { noti ->
                assertNotNull(noti)
                assertEquals(noti.type, "LendingConfirmed")
            }
        }
    }
}
