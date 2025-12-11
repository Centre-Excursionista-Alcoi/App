package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME

object FakeAdminUser : StubUser(
    FakeAdminUser.SUB,
    FakeAdminUser.NIF,
    FakeAdminUser.FULL_NAME,
    FakeAdminUser.EMAIL,
    FakeAdminUser.MEMBER_NUMBER,
    listOf(ADMIN_GROUP_NAME, "user"),
    FakeAdminUser.FCM_TOKEN,
) {
    const val SUB = "test-user-id-456"
    const val NIF = "11111111H"
    const val FULL_NAME = "Admin User"
    const val EMAIL = "admin@example.com"
    const val MEMBER_NUMBER = 1000u
    const val FCM_TOKEN = "token-fake-admin-user"
    val GROUPS = listOf(ADMIN_GROUP_NAME, "user")
}
