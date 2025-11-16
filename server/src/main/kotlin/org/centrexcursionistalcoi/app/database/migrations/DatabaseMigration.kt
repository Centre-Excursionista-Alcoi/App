package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

interface DatabaseMigration {
    val from: Int
    val to: Int

    context(_: JdbcTransaction)
    fun migrate()

    companion object {
        val migrations = listOf<DatabaseMigration>(
            V1,
        )

        /**
         * Finds the next migration starting from the given version.
         * @param from The current database version.
         * @return The next [DatabaseMigration] if available, otherwise null.
         */
        fun next(from: Int): DatabaseMigration? {
            return migrations.find { it.from == from }
        }
    }
}
