package org.centrexcursionistalcoi.app.authentik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthentikPagination(
    val next: Int,
    val previous: Int,
    val count: Int,
    val current: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("start_index") val startIndex: Int,
    @SerialName("end_index") val endIndex: Int
): AuthentikData
