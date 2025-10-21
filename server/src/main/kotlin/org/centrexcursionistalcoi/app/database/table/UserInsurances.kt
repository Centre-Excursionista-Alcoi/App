package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.integration.femecv.LicenseData
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.json

object UserInsurances : UUIDTable("UserInsurances") {
    /** The Subject Identifier of the user */
    val userSub = reference("sub", UserReferences, ReferenceOption.CASCADE, ReferenceOption.RESTRICT)
    val insuranceCompany = varchar("insuranceCompany", 255)
    val policyNumber = varchar("policyNumber", 255)
    val validFrom = date("validFrom")
    val validTo = date("validTo")
    val document = optReference("document", Files)

    val femecvLicenseLastUpdate = timestamp("femecv_license_last_update").nullable()
    val femecvLicense = json("femecv_license", json, LicenseData.serializer()).nullable()
}
