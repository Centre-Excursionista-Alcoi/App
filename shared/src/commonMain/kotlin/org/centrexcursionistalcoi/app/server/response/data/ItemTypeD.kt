package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class ItemTypeD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val title: String = "",
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val sectionId: Int? = null
): DatabaseData
