package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

object FakeUser2 {
    const val SUB = "test-user-id-456"
    const val NIF = "87654321X"
    const val FULL_NAME = "Sample User 2"
    const val EMAIL = "user2@example.com"
    const val MEMBER_NUMBER = 1002u
    val GROUPS = listOf("user")

    context(_: JdbcTransaction)
    fun provideEntity(): UserReferenceEntity = UserReferenceEntity.findById(SUB) ?: UserReferenceEntity.new(SUB) {
        nif = NIF
        fullName = FULL_NAME
        email = EMAIL
        groups = GROUPS
        memberNumber = MEMBER_NUMBER
    }
}
