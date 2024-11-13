package org.centrexcursionistalcoi.app.server.request

import kotlinx.serialization.Serializable

@Serializable
data class LendingRequest(
    val itemId: Int,
    val from: Long,
    val to: Long
)
