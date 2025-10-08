package org.centrexcursionistalcoi.app.database.utils

import kotlinx.serialization.SerializationStrategy
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.jdbc.SizedIterable

interface ViaLink<FromID: Any, FromEntity: Entity<FromID>, ToID: Any, ToEntity: Entity<ToID>> {
    val linkName: String

    fun linkSerializer(): Pair<SerializationStrategy<ToEntity>, Boolean>

    fun links(entity: FromEntity): SizedIterable<ToEntity>
}
