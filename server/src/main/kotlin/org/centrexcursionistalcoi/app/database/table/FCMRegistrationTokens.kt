package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens.id
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

object FCMRegistrationTokens : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("token", 512).entityId().uniqueIndex()
    /** Alias for [id]. */
    val token get() = id

    val user = reference("user", UserReferences)
    val deviceId = text("device_id").nullable()
}
