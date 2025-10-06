package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class LendingUserEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<LendingUser, Uuid> {
    companion object : UUIDEntityClass<LendingUserEntity>(LendingUsers)

    var userSub by LendingUsers.userSub

    var fullName by LendingUsers.fullName

    var nif by LendingUsers.nif
    var phoneNumber by LendingUsers.phoneNumber

    var sports by LendingUsers.sports

    var address by LendingUsers.address
    var postalCode by LendingUsers.postalCode
    var city by LendingUsers.city
    var province by LendingUsers.province
    var country by LendingUsers.country

    @ExperimentalUuidApi
    context(_: JdbcTransaction)
    override fun toData(): LendingUser = LendingUser(
        id = id.value.toKotlinUuid(),
        sub = userSub.value,
        fullName = fullName,
        nif = nif,
        phoneNumber = phoneNumber,
        sports = sports,
        address = address,
        postalCode = postalCode,
        city = city,
        province = province,
        country = country,
    )
}
