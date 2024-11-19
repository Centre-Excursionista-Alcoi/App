package org.centrexcursionistalcoi.app.server.request

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DateRangeRequest(
    val from: LocalDate,
    val to: LocalDate
)
