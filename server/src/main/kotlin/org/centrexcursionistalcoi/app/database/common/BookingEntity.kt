package org.centrexcursionistalcoi.app.database.common

import java.time.Instant
import java.time.LocalDate
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.database.entity.User
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID

abstract class BookingEntity<Serializable: DatabaseData>(id: EntityID<Int>): IntEntity(id), SerializableEntity<Serializable> {
    abstract var from: LocalDate
    abstract var to: LocalDate
    abstract var user: User
    abstract var confirmed: Boolean
    abstract var takenAt: Instant?
    abstract var returnedAt: Instant?
}