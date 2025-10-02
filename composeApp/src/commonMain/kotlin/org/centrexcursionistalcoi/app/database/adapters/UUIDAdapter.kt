package org.centrexcursionistalcoi.app.database.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlin.uuid.Uuid

object UUIDAdapter : ColumnAdapter<Uuid, String> {
    override fun decode(databaseValue: String): Uuid = Uuid.parse(databaseValue)

    override fun encode(value: Uuid): String = value.toString()
}
