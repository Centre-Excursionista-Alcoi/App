package org.centrexcursionistalcoi.app.database.adapters

import app.cash.sqldelight.ColumnAdapter
import org.centrexcursionistalcoi.app.data.ZonedDateTime

object ZonedDateTimeAdapter : ColumnAdapter<ZonedDateTime, String> {
    override fun decode(databaseValue: String): ZonedDateTime {
        return ZonedDateTime.parse(databaseValue)
    }

    override fun encode(value: ZonedDateTime): String {
        return value.toString()
    }
}