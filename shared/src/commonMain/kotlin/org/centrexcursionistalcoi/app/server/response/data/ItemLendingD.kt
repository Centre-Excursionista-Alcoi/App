package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class ItemLendingD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val itemIds: Set<Int>? = null,
    val userId: String? = null,
    val confirmed: Boolean = false,
    val from: Long? = null,
    val to: Long? = null,
    val takenAt: Long? = null,
    val returnedAt: Long? = null
): DatabaseData
