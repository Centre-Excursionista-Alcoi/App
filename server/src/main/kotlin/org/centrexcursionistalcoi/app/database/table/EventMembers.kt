package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object EventMembers : Table("event_members") {
    val event = reference("event_id", Events)
    val userReference = reference("user_reference", UserReferences)

    val timestamp = timestamp("timestamp").defaultExpression(DatabaseNowExpression)

    override val primaryKey = PrimaryKey(event, userReference, name = "PK_EventMembers_event_userReference")
}
