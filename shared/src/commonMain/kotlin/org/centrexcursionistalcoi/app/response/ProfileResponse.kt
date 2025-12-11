package org.centrexcursionistalcoi.app.response

import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserInsurance
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class)
@Serializable
data class ProfileResponse(
    val sub: String,
    val fullName: String,
    val memberNumber: UInt,
    val email: String,
    val groups: List<String>,
    val departments: List<Uuid>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>,
    val femecvSyncEnabled: Boolean,
    val femecvLastSync: Instant?,
) {
    val isAdmin: Boolean get() = ADMIN_GROUP_NAME in groups

    /**
     * Obtains a list of all the user's insurances active now in the given [timeZone].
     * @param clock The clock to get the current instant from.
     * @param timeZone The timezone to use. Set to `null` to use system default.
     */
    fun activeInsurances(clock: Clock = Clock.System, timeZone: TimeZone? = null) = insurances.filter { it.isActive(clock, timeZone) }

    /**
     * Obtains a list of all the user's insurances active at the given [instant] in the given [timeZone].
     * @param instant The instant to check.
     * @param timeZone The timezone to use. Set to `null` to use system default.
     */
    fun activeInsurances(instant: Instant, timeZone: TimeZone? = null) = insurances.filter { it.isActive(instant, timeZone) }
}
