package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.database.table.SpaceKeys
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeys
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration V13:
 * - The keys of the spaces work as an inventory: types of keys (`space_key_types`, for one or several spaces), the keys
 *   of the club (`space_keys`, each an individual copy of a type) and the keys a lending asks for
 *   (`space_lending_key_requests`) and takes (`space_lending_keys`).
 *
 * The keys were per space, with a maximum for each lending, and a lending took a quantity of them. That doesn't fit the
 * new ones, so `space_keys` and `space_lending_keys` are dropped and created again, without their rows. (Spaces were
 * still for admins only.) The new tables are created with the rest of the schema.
 */
object V13 : DatabaseMigration {
    override val from: Int = 12
    override val to: Int = 13

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("DROP TABLE IF EXISTS ${tr.identity(SpaceLendingKeys)}")
        tr.exec("DROP TABLE IF EXISTS ${tr.identity(SpaceKeys)}")
        // Only their own statements: creating a table also lists the indexes of the ones it references, which exist
        for (table in listOf(SpaceKeys, SpaceLendingKeys)) {
            val name = tr.identity(table)
            SchemaUtils.createStatements(table).filter { name in it }.forEach { tr.exec(it) }
        }
    }
}
