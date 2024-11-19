package org.centrexcursionistalcoi.app.database.common

import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.data.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID

abstract class SerializableEntity<SerializableType : DatabaseData>(
    id: EntityID<Int>
): IntEntity(id), Serializable<SerializableType>
