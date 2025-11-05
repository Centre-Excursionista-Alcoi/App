package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

object FakeUser {
    const val SUB = "test-user-id-123"
    const val USERNAME = "user"
    const val EMAIL = "user@example.com"
    val GROUPS = listOf("user")

    context(_: JdbcTransaction)
    fun provideEntity(): UserReferenceEntity = UserReferenceEntity.findById(SUB) ?: UserReferenceEntity.new(SUB) {
        username = USERNAME
        email = EMAIL
        groups = GROUPS
    }
}
