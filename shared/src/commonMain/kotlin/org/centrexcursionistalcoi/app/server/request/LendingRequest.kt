package org.centrexcursionistalcoi.app.server.request

import kotlinx.serialization.Serializable

@Serializable
data class LendingRequest(
    val from: Long,
    val to: Long,
    val itemIds: Set<Int>
)
