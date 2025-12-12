package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.integration.femecv.LicenseData
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.Uuid

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
    val femecvLicense: LicenseData? = null,
    val cardImage: String? = null,
): Entity<Uuid>, FileContainer {
    override val files: Map<String, Uuid?> = mapOf("documentId" to documentId)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userSub" to userSub,
        "insuranceCompany" to insuranceCompany,
        "policyNumber" to policyNumber,
        "validFrom" to validFrom.toString(),
        "validTo" to validTo.toString(),
        "documentId" to documentId?.let { FileReference(it) },
        "femecvLicense" to femecvLicense,
        "cardImage" to cardImage,
    )

    /**
     * Obtains a list of all the user's insurances active at the given [instant] in the given [timeZone].
     * @param instant The instant to check.
     * @param timeZone The timezone to use. Set to `null` to use system default.
     */
    fun isActive(instant: Instant, timeZone: TimeZone? = null): Boolean {
        val today = instant.toLocalDateTime(timeZone ?: TimeZone.currentSystemDefault()).date
        return (validFrom <= today) && (today <= validTo)
    }

    /**
     * Obtains a list of all the user's insurances active now in the given [timeZone].
     * @param clock The clock to get the current instant from.
     * @param timeZone The timezone to use. Set to `null` to use system default.
     */
    fun isActive(clock: Clock = Clock.System, timeZone: TimeZone? = null): Boolean {
        val now = clock.now()
        return isActive(now, timeZone)
    }
}
