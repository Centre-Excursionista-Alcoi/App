package org.centrexcursionistalcoi.app.notifications

import app.cash.turbine.test
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.notifications.Push.sendFCMPushNotification
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.utils.Zero
import org.centrexcursionistalcoi.app.utils.toUUID
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class TestPush {
    @Test
    fun `test flow for admins`() = runTest {
        val adminSession = UserSession(sub = "abc", fullName = "Admin", email = "admin@example.com", groups = listOf(ADMIN_GROUP_NAME))
        Push.flow(adminSession).test {
            // push notification includes admins, will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "def",
                includeAdmins = true,
            )

            awaitItem().let { noti ->
                assertNotNull(noti)
                assertEquals(noti.type, "LendingConfirmed")
            }


            // push notification doesn't include admins, and sub doesn't match. Won't be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "def",
                includeAdmins = false,
            )

            // should not emit anything
            expectNoEvents()


            // push notification doesn't include admins, and sub matches. Will be received
            Push.sendLocalPushNotification(
                notification = PushNotification.LendingConfirmed(lendingId = Uuid.Zero, "xyz"),
                userSub = "abc",
                includeAdmins = false,
            )

            awaitItem().let { noti ->
                assertNotNull(noti)
                assertEquals(noti.type, "LendingConfirmed")
            }
        }
    }

    private fun mockFCM(
        successCount: Int = 2,
        failureCount: Int = 0,
        initUsers: Boolean = true,
        block: suspend (FirebaseMessaging) -> Unit
    ) = runTest {
        try {
            if (initUsers) {
                Database.initForTests()
                Database { FakeUser.provideEntityWithFCMToken() }
                Database { FakeUser2.provideEntityWithFCMToken() }
                Database { FakeAdminUser.provideEntityWithFCMToken() }
            }

            mockkStatic(FirebaseMessaging::class)
            val batchResponseMock = mockk<BatchResponse> {
                every { this@mockk.failureCount } returns failureCount
                every { this@mockk.successCount } returns successCount
            }
            val messagingMock = mockk<FirebaseMessaging> {
                every { sendEachForMulticast(any()) } returns batchResponseMock
            }
            every { FirebaseMessaging.getInstance() } returns messagingMock
            Push.pushFCMConfigured = true

            block(messagingMock)
        } finally {
            unmockkStatic(FirebaseMessaging::class)
            Push.pushFCMConfigured = false
            Database.clear()
        }
    }

    @Test
    fun `test sendFCMPushNotification`() = mockFCM(initUsers = false) { messagingMock ->
        // -- Empty tokens list
        sendFCMPushNotification(emptySet(), mapOf("example" to "value"))
        // Since there are no tokens, sending should not be performed
        verify(exactly = 0) { messagingMock.sendEachForMulticast(any()) }

        // -- Non-empty tokens list
        sendFCMPushNotification(setOf(FakeUser.FCM_TOKEN, FakeAdminUser.FCM_TOKEN), mapOf("example" to "value"))
        verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
    }

    @Test
    fun `test sendAdminFCMPushNotification`() = mockFCM { messagingMock ->
        // Notification should only be sent to FakeAdminUser
        val pushSpy = spyk<Push>(recordPrivateCalls = true)
        pushSpy.sendAdminFCMPushNotification(mapOf("example" to "value"))
        verify { pushSpy.sendFCMPushNotification(setOf(FakeAdminUser.FCM_TOKEN), any()) }
        verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
    }

    private val testNotifications get() = sequenceOf(
        PushNotification.NewLendingRequest(Uuid.Zero, FakeUser.SUB),
        PushNotification.NewMemoryUpload(Uuid.Zero, FakeUser.SUB),
        PushNotification.LendingCancelled(Uuid.Zero, FakeUser.SUB),
        PushNotification.LendingConfirmed(Uuid.Zero, FakeUser.SUB),
        PushNotification.LendingTaken(Uuid.Zero, FakeUser.SUB),
        PushNotification.LendingPartiallyReturned(Uuid.Zero, FakeUser.SUB),
        PushNotification.LendingReturned(Uuid.Zero, FakeUser.SUB),
        PushNotification.NewPost(Uuid.Zero),
        PushNotification.DepartmentJoinRequestUpdated(Uuid.Zero, Uuid.Zero, FakeUser.SUB, true),
        PushNotification.DepartmentKicked(Uuid.Zero, Uuid.Zero, FakeUser.SUB),
        PushNotification.NewEvent(Uuid.Zero),
    )

    @TestFactory
    fun `test sendAdminPushNotification`(): List<DynamicTest> = testNotifications.map { notification ->
        DynamicTest.dynamicTest("for ${notification.type}") {
            mockFCM { messagingMock ->
                val pushSpy = spyk<Push>(recordPrivateCalls = true)
                pushSpy.sendAdminPushNotification(notification)
                // Notification should only be sent to FakeAdminUser, even though it's targeted to FakeUser
                verify { pushSpy.sendFCMPushNotification(setOf(FakeAdminUser.FCM_TOKEN), notification.toMap()) }
                verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
            }
        }
    }.toList()

    @TestFactory
    fun `test sendPushNotification includeAdmins=true`(): List<DynamicTest> = testNotifications.map { notification ->
        DynamicTest.dynamicTest("for ${notification.type}") {
            mockFCM { messagingMock ->
                val pushSpy = spyk<Push>(recordPrivateCalls = true)
                pushSpy.sendPushNotification(
                    FakeUser.SUB,
                    notification,
                    includeAdmins = true,
                )
                // Notification should only be sent to FakeUser and FakeAdminUser
                verify {
                    pushSpy.sendFCMPushNotification(
                        setOf(FakeUser.FCM_TOKEN, FakeAdminUser.FCM_TOKEN),
                        notification.toMap()
                    )
                }
                verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
            }
        }
    }.toList()

    @TestFactory
    fun `test sendPushNotification includeAdmins=false`(): List<DynamicTest> = testNotifications.map { notification ->
        DynamicTest.dynamicTest("for ${notification.type}") {
            mockFCM { messagingMock ->
                val pushSpy = spyk<Push>(recordPrivateCalls = true)
                pushSpy.sendPushNotification(
                    FakeUser.SUB,
                    notification,
                    includeAdmins = false,
                )
                // Notification should only be sent to FakeUser
                verify { pushSpy.sendFCMPushNotification(setOf(FakeUser.FCM_TOKEN), notification.toMap()) }
                verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
            }
        }
    }.toList()

    @TestFactory
    fun `test sendPushNotificationToDepartment`(): List<DynamicTest> = testNotifications
        .flatMap { sequenceOf(it to true, it to false) }
        .map { (notification, includeAdmins) ->
            DynamicTest.dynamicTest("includeAdmins=$includeAdmins for ${notification.type}") {
                mockFCM { messagingMock ->
                    val department = Database {
                        DepartmentEntity.new("2c216742-f008-4aaf-9450-c8dfb644e094".toUUID()) {
                            displayName = "Department"
                        }
                    }
                    department.updated()

                    Database {
                        DepartmentMemberEntity.new {
                            this.department = department
                            this.userSub = FakeUser.provideEntity().sub
                            this.confirmed = true
                        }
                        // This request has not been confirmed, should not receive notification
                        DepartmentMemberEntity.new {
                            this.department = department
                            this.userSub = FakeUser2.provideEntity().sub
                            this.confirmed = false
                        }
                    }

                    val pushSpy = spyk<Push>(recordPrivateCalls = true)
                    pushSpy.sendPushNotificationToDepartment(notification, department.id.value, includeAdmins)
                    // Notification should only be sent to FakeUser and FakeAdminUser
                    verify {
                        pushSpy.sendFCMPushNotification(
                            if (includeAdmins)
                                setOf(
                                    FakeUser.FCM_TOKEN,
                                    FakeAdminUser.FCM_TOKEN
                                )
                            else
                                setOf(FakeUser.FCM_TOKEN),
                            notification.toMap()
                        )
                    }
                    verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
                }
            }
        }.toList()

    @TestFactory
    fun `test sendPushNotificationToAll`(): List<DynamicTest> = testNotifications.map { notification ->
        DynamicTest.dynamicTest("for ${notification.type}") {
            mockFCM { messagingMock ->
                val pushSpy = spyk<Push>(recordPrivateCalls = true)
                pushSpy.sendPushNotificationToAll(notification)
                // Notification should only be sent to FakeUser and FakeAdminUser
                verify {
                    pushSpy.sendFCMPushNotification(
                        setOf(
                            FakeUser.FCM_TOKEN,
                            FakeUser2.FCM_TOKEN,
                            FakeAdminUser.FCM_TOKEN
                        ),
                        notification.toMap()
                    )
                }
                verify(exactly = 1) { messagingMock.sendEachForMulticast(any()) }
            }
        }
    }.toList()
}
