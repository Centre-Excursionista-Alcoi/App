package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ItemLendingD(
    override val id: Int? = null,
    val createdAt: Instant? = null,
    val itemIds: Set<Int>? = null,
    override val userId: String? = null,
    override val confirmed: Boolean = false,
    override val from: LocalDate? = null,
    override val to: LocalDate? = null,
    override val takenAt: Instant? = null,
    override val returnedAt: Instant? = null
): IBookingD
