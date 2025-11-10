package org.centrexcursionistalcoi.app.database.adapters

import app.cash.sqldelight.ColumnAdapter

object ListStringAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> = databaseValue.split('\n')

    override fun encode(value: List<String>): String = value.joinToString("\n")
}
