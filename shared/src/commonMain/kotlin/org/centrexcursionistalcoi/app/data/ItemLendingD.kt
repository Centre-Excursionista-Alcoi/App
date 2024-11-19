package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ItemLendingD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val itemIds: Set<Int>? = null,
    override val userId: String? = null,
    override val confirmed: Boolean = false,
    override val from: Long? = null,
    override val to: Long? = null,
    override val takenAt: Long? = null,
    override val returnedAt: Long? = null
): IBookingD
