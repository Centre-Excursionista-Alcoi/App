package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import kotlin.uuid.Uuid

@Entity(tableName = "SpaceKeyTypes")
data class SpaceKeyTypeEntity(
    @PrimaryKey
    val id: Uuid,
    val name: String,
    val description: String?,
    val spaces: List<SpaceKeyTypeSpace>,
) {
    fun toSpaceKeyType() = SpaceKeyType(
        id = id,
        name = name,
        description = description,
        spaces = spaces,
    )

    companion object {
        fun SpaceKeyType.toEntity() = SpaceKeyTypeEntity(
            id = id,
            name = name,
            description = description,
            spaces = spaces,
        )
    }
}
