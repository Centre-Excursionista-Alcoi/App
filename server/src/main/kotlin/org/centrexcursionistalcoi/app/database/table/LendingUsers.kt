package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.Sports
import org.jetbrains.exposed.v1.core.EnumerationNameColumnType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

/**
 * Holds a list of all the users that have accepted the conditions for the lending service.
 */
object LendingUsers : UUIDTable("lendingUsers") {
    /** The Subject Identifier of the user */
    val userSub = reference("sub", UserReferences, ReferenceOption.CASCADE, ReferenceOption.RESTRICT).uniqueIndex()

    val phoneNumber = text("phoneNumber")

    val sports = array("sports", EnumerationNameColumnType(Sports::class, 32))
}
