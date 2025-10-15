package org.centrexcursionistalcoi.app.test

import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object FakeAdminUser {
    const val SUB = "test-user-id-456"
    const val USERNAME = "admin"
    const val EMAIL = "admin@example.com"
    val GROUPS = listOf(ADMIN_GROUP_NAME, "user")

    context(_: JdbcTransaction)
    fun provideEntity(): UserReferenceEntity = transaction {
        UserReferenceEntity.new(SUB) {
            username = USERNAME
            email = EMAIL
            groups = GROUPS
        }
    }
}
