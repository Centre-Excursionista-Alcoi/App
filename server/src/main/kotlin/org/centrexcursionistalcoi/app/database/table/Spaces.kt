package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.duration

object Spaces : UUIDTable("spaces") {
    val name = varchar("name", 255)
    val description = text("description").nullable()

    val price = decimal("price", 10, 2).nullable()
    val priceDuration = duration("price_duration").nullable()

    val capacity = integer("capacity").nullable()

    init {
        // price and priceDuration must be both null or both not null
        check("chk_price_priceDuration") {
            (price.isNull() and priceDuration.isNull()) or (price.isNotNull() and priceDuration.isNotNull())
        }
    }
}
