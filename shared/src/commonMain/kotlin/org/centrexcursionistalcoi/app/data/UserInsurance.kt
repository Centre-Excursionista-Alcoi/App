package org.centrexcursionistalcoi.app.data

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.integration.femecv.LicenseData

@OptIn(ExperimentalTime::class)
@Serializable
data class UserInsurance(
    override val id: Uuid,
    val userSub: String,
    val insuranceCompany: String,
    val policyNumber: String,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val documentId: Uuid?,
    val femecvLicense: LicenseData? = null
): Entity<Uuid>, FileContainer {
    override val files: Map<String, Uuid?> = mapOf("documentId" to documentId)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userSub" to userSub,
        "insuranceCompany" to insuranceCompany,
        "policyNumber" to policyNumber,
        "validFrom" to validFrom.toString(),
        "validTo" to validTo.toString(),
        "documentId" to documentId?.let { FileReference(it) }
    )

    fun isActive(clock: Clock = Clock.System): Boolean {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return (validFrom <= today) && (today <= validTo)
    }
}
