package org.centrexcursionistalcoi.app.response

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserInsurance

@OptIn(ExperimentalTime::class)
@Serializable
data class ProfileResponse(
    val sub: String,
    val username: String,
    val email: String,
    val groups: List<String>,
    val departments: List<Int>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>,
    val femecvSyncEnabled: Boolean,
    val femecvLastSync: Instant?,
) {
    val isAdmin: Boolean get() = ADMIN_GROUP_NAME in groups

    fun activeInsurances(clock: Clock = Clock.System) = insurances.filter { it.isActive(clock) }
}
