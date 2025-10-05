package org.centrexcursionistalcoi.app.response

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserInsurance
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
data class ProfileResponse(
    val username: String,
    val email: String,
    val groups: List<String>,
    val departments: List<Int>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>
) {
    val isAdmin: Boolean get() = ADMIN_GROUP_NAME in groups

    fun activeInsurances(clock: Clock = Clock.System) = insurances.filter { it.isActive(clock) }
}
