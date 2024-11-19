package org.centrexcursionistalcoi.app.server.request

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class LendingRequest(
    val from: LocalDate,
    val to: LocalDate,
    val itemIds: Set<Int>
)
