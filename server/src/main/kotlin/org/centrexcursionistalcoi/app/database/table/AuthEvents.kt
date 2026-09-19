package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.AuthEventType
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Records every authentication-related request (login, registration, password recovery...), successful or not,
 * so that a support report ("I'm not able to register") can be investigated after the fact.
 */
object AuthEvents : UUIDTable("auth_events") {
    val timestamp = timestamp("timestamp").defaultExpression(DatabaseNowExpression)

    /** Name of an [AuthEventType] constant. */
    val type = enumerationByName<AuthEventType>("type", 32)

    /** The email involved in the request, if known at the point of failure/success. Not normalized/validated. */
    val email = text("email").nullable()

    val success = bool("success")

    /** [org.centrexcursionistalcoi.app.error.Error.code] of the failure, `null` on success. */
    val errorCode = integer("error_code").nullable()

    /** [org.centrexcursionistalcoi.app.error.Error.description] of the failure, `null` on success. */
    val errorDescription = text("error_description").nullable()

    val ipAddress = varchar("ip_address", 64).nullable()
    val userAgent = text("user_agent").nullable()
}
