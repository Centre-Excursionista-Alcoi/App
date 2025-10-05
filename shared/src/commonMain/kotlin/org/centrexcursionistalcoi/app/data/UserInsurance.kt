package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class UserInsurance(
    val id: Uuid,
    val userSub: String,
    val insuranceCompany: String,
    val policyNumber: String,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val documentId: Uuid?
)
