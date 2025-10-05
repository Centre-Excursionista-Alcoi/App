package org.centrexcursionistalcoi.app.database.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.json

class JsonAdapter<T : Any>(
    private val serializer: KSerializer<T>,
) : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String): T = json.decodeFromString(serializer, databaseValue)

    override fun encode(value: T): String = json.encodeToString(serializer, value)
}
