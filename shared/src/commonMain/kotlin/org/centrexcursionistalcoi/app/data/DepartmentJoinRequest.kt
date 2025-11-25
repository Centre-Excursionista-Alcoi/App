package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class DepartmentJoinRequest(
    val userSub: String,
    val department: Uuid,
    val requestId: Uuid
)
