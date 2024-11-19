package org.centrexcursionistalcoi.app.server.request

import kotlinx.serialization.Serializable

@Serializable
data class DateRangeRequest(
    val from: Long,
    val to: Long
)
