package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.Space
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "Spaces")
data class SpaceEntity(
    @PrimaryKey
    val id: Uuid,
    val name: String,
    val description: String,
    val conditionsOfUse: String?,
    val requiresKeys: Boolean,
    val prices: List<CategoryPrice>,
    val isClosed: Boolean,
    val closedSince: Instant?,
    val closedUntil: Instant?,
    val closedReason: String?,
) {
    fun toSpace() = Space(
        id = id,
        name = name,
        description = description,
        conditionsOfUse = conditionsOfUse,
        requiresKeys = requiresKeys,
        prices = prices,
        isClosed = isClosed,
        closedSince = closedSince,
        closedUntil = closedUntil,
        closedReason = closedReason,
    )

    companion object {
        fun Space.toEntity() = SpaceEntity(
            id = id,
                name = name,
            description = description,
            conditionsOfUse = conditionsOfUse,
            requiresKeys = requiresKeys,
            prices = prices,
            isClosed = isClosed,
            closedSince = closedSince,
            closedUntil = closedUntil,
            closedReason = closedReason,
        )
    }
}
