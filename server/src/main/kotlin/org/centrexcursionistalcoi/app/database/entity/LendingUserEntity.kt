package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class LendingUserEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<LendingUser, Uuid> {
    companion object : UUIDEntityClass<LendingUserEntity>(LendingUsers)

    var userSub by UserReferenceEntity referencedOn LendingUsers.userSub

    var phoneNumber by LendingUsers.phoneNumber

    var sports by LendingUsers.sports

    @ExperimentalUuidApi
    context(_: JdbcTransaction)
    override fun toData(): LendingUser = LendingUser(
        id = id.value.toKotlinUuid(),
        sub = userSub.id.value,
        phoneNumber = phoneNumber,
        sports = sports,
    )
}
