package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.security.Permissions

/**
 * Fake user with lending permissions for testing.
 * Has permissions to give and receive items that are not linked to any department.
 */
object FakeLendingUser : StubUser(
    FakeLendingUser.SUB,
    FakeLendingUser.NIF,
    FakeLendingUser.FULL_NAME,
    FakeLendingUser.EMAIL,
    FakeLendingUser.MEMBER_NUMBER,
    listOf(
        "user",
        Permissions.Lending.GIVE_NO_DEPARTMENT,
        Permissions.Lending.RECEIVE_NO_DEPARTMENT
    ),
    arrayOf(FakeLendingUser.FCM_TOKEN1, FakeLendingUser.FCM_TOKEN2),
) {
    const val SUB = "test-lending-user-id-789"
    const val NIF = "22222222Y"
    const val FULL_NAME = "Lending User"
    const val EMAIL = "lending@example.com"
    const val MEMBER_NUMBER = 1002u
    const val FCM_TOKEN1 = "token-fake-lending-user-token1"
    const val FCM_TOKEN2 = "token-fake-lending-user-token2"
    val GROUPS = listOf(
        "user",
        Permissions.Lending.GIVE_NO_DEPARTMENT,
        Permissions.Lending.RECEIVE_NO_DEPARTMENT
    )
}
