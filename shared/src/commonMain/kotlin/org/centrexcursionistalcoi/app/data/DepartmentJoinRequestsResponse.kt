package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class DepartmentJoinRequestsResponse(
    val requests: List<Request>
) {
    @Serializable
    data class Request(
        val userSub: String,
        val requestId: Uuid
    )
}
