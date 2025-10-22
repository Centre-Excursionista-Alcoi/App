package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

class FCMRegistrationTokenEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FCMRegistrationTokenEntity>(FCMRegistrationTokens)

    var user by UserReferenceEntity referencedOn FCMRegistrationTokens.user
    var token by FCMRegistrationTokens.token
    var deviceId by FCMRegistrationTokens.deviceId
}
