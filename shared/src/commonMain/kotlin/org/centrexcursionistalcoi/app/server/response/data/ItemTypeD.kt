package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class ItemTypeD(
    val id: Int? = null,
    val createdAt: Long,
    val title: String,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null
)
