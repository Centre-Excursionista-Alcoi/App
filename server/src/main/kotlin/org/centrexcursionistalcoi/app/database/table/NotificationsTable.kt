package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.push.payload.PushPayload
import org.centrexcursionistalcoi.app.serverJson
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.json

object NotificationsTable : IntIdTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val viewed = bool("viewed").default(false)
    val type = enumeration("type", NotificationType::class)
    val payload = json<PushPayload>("payload", serverJson)

    val userId = reference("userId", UsersTable)
}
