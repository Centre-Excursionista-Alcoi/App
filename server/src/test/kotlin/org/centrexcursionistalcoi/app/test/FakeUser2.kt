package org.centrexcursionistalcoi.app.test

object FakeUser2 : StubUser(
    FakeUser2.SUB,
    FakeUser2.NIF,
    FakeUser2.FULL_NAME,
    FakeUser2.EMAIL,
    FakeUser2.MEMBER_NUMBER,
    listOf("user")
) {
    const val SUB = "test-user-id-456"
    const val NIF = "87654321X"
    const val FULL_NAME = "Sample User 2"
    const val EMAIL = "user2@example.com"
    const val MEMBER_NUMBER = 1002u
}
