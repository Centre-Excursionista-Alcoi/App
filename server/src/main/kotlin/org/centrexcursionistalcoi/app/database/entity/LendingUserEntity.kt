package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class LendingUserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<LendingUserEntity>(LendingUsers)

    var userSub by LendingUsers.userSub
    var nif by LendingUsers.nif
    var phoneNumber by LendingUsers.phoneNumber

    @ExperimentalUuidApi
    context(_: JdbcTransaction)
    fun toData(): LendingUser = LendingUser(
        id = id.value.toKotlinUuid(),
        sub = userSub,
        nif = nif,
        phoneNumber = phoneNumber
    )
}
