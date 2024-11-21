package org.centrexcursionistalcoi.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.enumeration.ItemHealth

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ItemType::class,
            parentColumns = ["id"],
            childColumns = ["itemTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Item(
    @PrimaryKey override val id: Int = 0,
    override val createdAt: Instant = Clock.System.now(),
    val health: ItemHealth = ItemHealth.NEW,
    val notes: String? = null,
    @ColumnInfo(index = true) val itemTypeId: Int = 0
): DatabaseEntity<ItemD> {
    companion object : EntityDeserializer<ItemD, Item> {
        override fun deserialize(source: ItemD): Item {
            return Item(
                id = source.id!!,
                createdAt = Instant.fromEpochMilliseconds(source.createdAt!!),
                health = source.health,
                notes = source.notes,
                itemTypeId = source.typeId!!
            )
        }
    }

    override fun serializable(): ItemD {
        return ItemD(
            id = id.takeIf { it > 0 },
            createdAt = createdAt.toEpochMilliseconds(),
            health = health,
            notes = notes,
            typeId = itemTypeId
        )
    }

    override fun validate(): Boolean {
        return serializable().validate()
    }
}
