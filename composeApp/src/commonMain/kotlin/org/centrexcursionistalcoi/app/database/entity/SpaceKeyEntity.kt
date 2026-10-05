package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.SpaceKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "SpaceKeys",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["space"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["space"])],
)
data class SpaceKeyEntity(
    @PrimaryKey
    val id: Uuid,
    val space: Uuid,
    val name: String,
    val maxQuantity: Int,
    val nfcId: ByteArray?,
) {
    fun toSpaceKey() = SpaceKey(
        id = id,
        space = space,
        name = name,
        maxQuantity = maxQuantity,
        nfcId = nfcId,
    )

    override fun equals(other: Any?): Boolean = other is SpaceKeyEntity && toSpaceKey() == other.toSpaceKey()

    override fun hashCode(): Int = toSpaceKey().hashCode()

    companion object {
        fun SpaceKey.toEntity() = SpaceKeyEntity(
            id = id,
                space = space,
            name = name,
            maxQuantity = maxQuantity,
            nfcId = nfcId,
        )
    }
}
