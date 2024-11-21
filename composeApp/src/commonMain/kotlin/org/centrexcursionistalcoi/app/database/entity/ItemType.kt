package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.ItemTypeD

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ItemType(
    @PrimaryKey override val id: Int = 0,
    override val createdAt: Instant = Clock.System.now(),
    val title: String = "",
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val image: ByteArray? = null,
    val sectionId: Int = 0
) : DatabaseEntity<ItemTypeD> {
    companion object : EntityDeserializer<ItemTypeD, ItemType> {
        @OptIn(ExperimentalEncodingApi::class)
        override fun deserialize(source: ItemTypeD): ItemType {
            return ItemType(
                id = source.id!!,
                createdAt = Instant.fromEpochMilliseconds(source.createdAt!!),
                title = source.title,
                description = source.description,
                brand = source.brand,
                model = source.model,
                image = source.imageBytes(),
                sectionId = source.sectionId!!
            )
        }
    }

    override fun serializable(): ItemTypeD {
        return ItemTypeD(
            id = id.takeIf { it > 0 },
            createdAt = createdAt.toEpochMilliseconds(),
            title = title,
            description = description,
            brand = brand,
            model = model,
            sectionId = sectionId
        )
    }

    override fun validate(): Boolean {
        return serializable().validate()
    }
}
