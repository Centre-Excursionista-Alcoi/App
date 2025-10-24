package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

class FCMRegistrationTokenEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, FCMRegistrationTokenEntity>(FCMRegistrationTokens)

    /** Alias for [id]. */
    val token by FCMRegistrationTokens.token
    var user by UserReferenceEntity referencedOn FCMRegistrationTokens.user
    var deviceId by FCMRegistrationTokens.deviceId
}
