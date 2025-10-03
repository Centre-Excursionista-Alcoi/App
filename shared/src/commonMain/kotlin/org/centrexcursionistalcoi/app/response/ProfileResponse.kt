package org.centrexcursionistalcoi.app.response

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.LendingUser

@Serializable
data class ProfileResponse(
    val username: String,
    val email: String,
    val groups: List<String>,
    val departments: List<Int>,
    val lendingUser: LendingUser?
) {
    val isAdmin: Boolean get() = ADMIN_GROUP_NAME in groups
}
