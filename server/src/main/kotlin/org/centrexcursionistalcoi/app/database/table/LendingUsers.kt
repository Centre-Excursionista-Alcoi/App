package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

/**
 * Holds a list of all the users that have accepted the conditions for the lending service.
 */
object LendingUsers : UUIDTable("lendingUsers") {
    /** The Subject Identifier of the user */
    val userSub = text("sub").uniqueIndex()

    val nif = text("nif")
    val phoneNumber = text("phoneNumber")
}
