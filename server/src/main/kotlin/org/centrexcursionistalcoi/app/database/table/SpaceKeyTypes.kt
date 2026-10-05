package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * A kind of key the club has to access spaces (see [org.centrexcursionistalcoi.app.data.SpaceKeyType]). The keys are
 * in [SpaceKeys], and the spaces it gives access to in [SpaceKeyTypeSpaces].
 */
object SpaceKeyTypes : UuidTable("space_key_types") {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val name = text("name")
    val description = text("description").nullable()
}
