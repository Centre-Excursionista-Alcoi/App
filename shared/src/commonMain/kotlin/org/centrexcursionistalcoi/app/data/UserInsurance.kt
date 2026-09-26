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
): Entity<Uuid>, DocumentFileContainer {
    override val documentFile: Uuid? = documentId

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

    /** Whether this insurance covers [today], hasn't started yet, or has already ended. */
    fun status(today: LocalDate): Status = when {
        today < validFrom -> Status.UPCOMING
        today > validTo -> Status.EXPIRED
        else -> Status.ACTIVE
    }

    /** Declared in display order: active insurances first, then upcoming, then expired. */
    enum class Status { ACTIVE, UPCOMING, EXPIRED }
}

/**
 * Sorts insurances for display relative to [today]: active ones first (the soonest to expire on top), then upcoming
 * ones (the soonest to start on top), then expired ones (the most recently expired on top).
 */
fun List<UserInsurance>.sortedForDisplay(today: LocalDate): List<UserInsurance> = sortedWith(
    compareBy<UserInsurance> { it.status(today) }.thenBy { insurance ->
        when (insurance.status(today)) {
            UserInsurance.Status.ACTIVE -> insurance.validTo.toEpochDays()
            UserInsurance.Status.UPCOMING -> insurance.validFrom.toEpochDays()
            UserInsurance.Status.EXPIRED -> -insurance.validTo.toEpochDays()
        }
    }
)
