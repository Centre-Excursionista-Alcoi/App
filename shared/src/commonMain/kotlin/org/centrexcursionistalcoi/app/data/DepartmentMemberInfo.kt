package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class DepartmentMemberInfo(
    val userSub: String,
    val departmentId: Int,
    val confirmed: Boolean,
)
