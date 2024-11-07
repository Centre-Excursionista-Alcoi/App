package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class SectionD(
    val id: Int? = null,
    val createdAt: Long? = null,
    val displayName: String
)
