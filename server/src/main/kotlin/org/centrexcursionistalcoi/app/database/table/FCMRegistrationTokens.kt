package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object FCMRegistrationTokens : UUIDTable() {
    val user = reference("user", UserReferences)
    val token = varchar("token", 512).uniqueIndex()
    val deviceId = text("device_id").nullable()
}
