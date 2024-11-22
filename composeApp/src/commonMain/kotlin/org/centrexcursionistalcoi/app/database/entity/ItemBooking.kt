package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.ItemLendingD

@Entity
data class ItemBooking(
    @PrimaryKey override val id: Int,
    override val createdAt: Instant,
    val itemIds: List<Int>,
    override val userId: String,
    override val confirmed: Boolean,
    override val from: LocalDate,
    override val to: LocalDate,
    override val takenAt: Instant? = null,
    override val returnedAt: Instant? = null
): BookingEntity<ItemLendingD> {
    companion object : EntityDeserializer<ItemLendingD, ItemBooking> {
        override fun deserialize(source: ItemLendingD): ItemBooking {
            return ItemBooking(
                id = source.id!!,
                createdAt = source.createdAt!!,
                itemIds = source.itemIds!!.toList(),
                userId = source.userId!!,
                confirmed = source.confirmed,
                from = source.from!!,
                to = source.to!!,
                takenAt = source.takenAt,
                returnedAt = source.returnedAt
            )
        }
    }

    override fun serializable(): ItemLendingD {
        return ItemLendingD(
            id = id.takeIf { it > 0 },
            createdAt = createdAt,
            itemIds = itemIds.toSet(),
            userId = userId,
            confirmed = confirmed,
            from = from,
            to = to,
            takenAt = takenAt,
            returnedAt = returnedAt
        )
    }

    override fun validate(): Boolean = true
}
