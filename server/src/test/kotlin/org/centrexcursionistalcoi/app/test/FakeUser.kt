package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.now
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

object FakeUser {
    const val SUB = "test-user-id-123"
    const val NIF = "12345678Z"
    const val FULL_NAME = "Sample User"
    const val EMAIL = "user@example.com"
    const val MEMBER_NUMBER = 1001u
    val GROUPS = listOf("user")

    context(_: JdbcTransaction)
    fun provideEntity(): UserReferenceEntity = UserReferenceEntity.findById(SUB) ?: UserReferenceEntity.new(SUB) {
        nif = NIF
        fullName = FULL_NAME
        email = EMAIL
        groups = GROUPS
        memberNumber = MEMBER_NUMBER
        lastUpdate = now()
    }
}
