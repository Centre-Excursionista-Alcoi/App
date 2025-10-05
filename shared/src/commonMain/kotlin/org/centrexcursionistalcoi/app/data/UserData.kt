package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val sub: String,
    val username: String,
    val email: String,
    val groups: List<String>,
    val departments: List<DepartmentMemberInfo>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>
)
