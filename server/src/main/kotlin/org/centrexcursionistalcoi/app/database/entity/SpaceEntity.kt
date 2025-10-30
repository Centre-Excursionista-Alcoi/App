package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.table.Spaces
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class SpaceEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Space, Uuid> {
    companion object : UUIDEntityClass<SpaceEntity>(Spaces)

    var name by Spaces.name
    var description by Spaces.description

    var price by Spaces.price
    var priceDuration by Spaces.priceDuration

    var capacity by Spaces.capacity

    context(_: JdbcTransaction)
    override fun toData(): Space = Space(
        id = id.value.toKotlinUuid(),
        name = name,
        description = description,
        price = if (price != null && priceDuration != null) {
            Pair(price!!.toDouble(), priceDuration!!.toKotlinDuration())
        } else {
            null
        },
        capacity = capacity,
    )
}
