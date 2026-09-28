package org.centrexcursionistalcoi.app.database.entity

import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class LendingUserEntity(id: EntityID<Uuid>) : UuidEntity(id), EntityDataConverter<LendingUser, Uuid> {
    companion object : UuidEntityClass<LendingUserEntity>(LendingUsers)

    var userSub by UserReferenceEntity referencedOn LendingUsers.userSub

    var phoneNumber by LendingUsers.phoneNumber

    var sports by LendingUsers.sports

    @ExperimentalUuidApi
    context(_: JdbcTransaction)
    override fun toData(): LendingUser = LendingUser(
        id = id.value,
        sub = userSub.id.value,
        phoneNumber = phoneNumber,
        sports = sports,
    )
}
