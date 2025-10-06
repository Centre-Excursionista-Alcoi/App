package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Entity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

interface EntityDataConverter<DataEntity : Entity<IdType>, IdType: Any> {
    context(_: JdbcTransaction)
    fun toData(): DataEntity
}
