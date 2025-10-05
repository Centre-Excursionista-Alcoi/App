package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class)
@Serializable
data class UserInsurance(
    val id: Uuid,
    val userSub: String,
    val insuranceCompany: String,
    val policyNumber: String,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val documentId: Uuid?
) {
    fun isActive(clock: Clock = Clock.System): Boolean {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return (validFrom <= today) && (today <= validTo)
    }
}
