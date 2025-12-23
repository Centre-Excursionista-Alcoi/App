package org.centrexcursionistalcoi.app.test

object FakeUser : StubUser(
    FakeUser.SUB,
    FakeUser.NIF,
    FakeUser.FULL_NAME,
    FakeUser.EMAIL,
    FakeUser.MEMBER_NUMBER,
    listOf("user"),
    arrayOf(FakeUser.FCM_TOKEN1, FakeUser.FCM_TOKEN2),
) {
    const val SUB = "test-user-id-123"
    const val NIF = "12345678Z"
    const val FULL_NAME = "Sample User"
    const val EMAIL = "user@example.com"
    const val MEMBER_NUMBER = 1001u
    const val FCM_TOKEN1 = "token-fake-user-token1"
    const val FCM_TOKEN2 = "token-fake-user-token2"
    val GROUPS = listOf("user")
}
