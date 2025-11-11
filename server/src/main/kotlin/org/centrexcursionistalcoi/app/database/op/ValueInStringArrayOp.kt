package org.centrexcursionistalcoi.app.database.op

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.TextColumnType

class ValueInStringArrayOp(
    private val value: String,
    private val arrayColumn: Column<List<String>>
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        // produce: ? = ANY(<column>)
        registerArgument(TextColumnType(), value)   // registers the single value as VARCHAR
        append(" = ANY(")
        append(arrayColumn.table.nameInDatabaseCase() + ".\"" + arrayColumn.nameUnquoted() + "\"")
        append(")")
    }
}
