package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.date

object UserInsurances : UUIDTable("UserInsurances") {
    /** The Subject Identifier of the user */
    val userSub = text("sub")
    val insuranceCompany = varchar("insuranceCompany", 255)
    val policyNumber = varchar("policyNumber", 255)
    val validFrom = date("validFrom")
    val validTo = date("validTo")
    val document = optReference("document", Files)
}
