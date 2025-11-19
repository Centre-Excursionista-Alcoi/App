package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class DepartmentMemberInfo(
    val userSub: String,
    val departmentId: Uuid,
    val confirmed: Boolean,
)
