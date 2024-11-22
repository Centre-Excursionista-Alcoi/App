package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.DatabaseData

interface EntityDeserializer<Source: DatabaseData, Target: DatabaseEntity<Source>> {
    fun deserialize(source: Source): Target
}
