package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.timestamp

object RecoverPasswordRequests : IdTable<String>("recover_password_requests") {
    override val id = varchar("id", 128).entityId()
    val timestamp = timestamp("timestamp").defaultExpression(DatabaseNowExpression)
    val user = reference("user", UserReferences)
    val redirectTo = varchar("redirect_to", 512).nullable()
}
