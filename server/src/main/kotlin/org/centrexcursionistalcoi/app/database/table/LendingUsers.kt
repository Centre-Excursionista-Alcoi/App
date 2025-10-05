package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.Sports
import org.jetbrains.exposed.v1.core.EnumerationNameColumnType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

/**
 * Holds a list of all the users that have accepted the conditions for the lending service.
 */
object LendingUsers : UUIDTable("lendingUsers") {
    /** The Subject Identifier of the user */
    val userSub = reference("sub", UserReferences, ReferenceOption.CASCADE, ReferenceOption.RESTRICT).uniqueIndex()

    val fullName = text("fullName")
    val nif = text("nif")

    val phoneNumber = text("phoneNumber")

    val sports = array("sports", EnumerationNameColumnType(Sports::class, 32))

    val address = text("address")
    val postalCode = text("postalCode")
    val city = text("city")
    val province = text("province")
    val country = text("country")
}
