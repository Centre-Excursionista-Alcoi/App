package org.centrexcursionistalcoi.app.test

import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.now
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object FakeAdminUser {
    const val SUB = "test-user-id-456"
    const val NIF = "87654321X"
    const val FULL_NAME = "Admin User"
    const val EMAIL = "admin@example.com"
    const val MEMBER_NUMBER = 1000u
    val GROUPS = listOf(ADMIN_GROUP_NAME, "user")

    context(_: JdbcTransaction)
    fun provideEntity(): UserReferenceEntity = transaction {
        UserReferenceEntity.findById(SUB) ?: UserReferenceEntity.new(SUB) {
            nif = NIF
            fullName = FULL_NAME
            email = EMAIL
            groups = GROUPS
            memberNumber = MEMBER_NUMBER
        }.also { runBlocking { it.updated() } }
    }
}
