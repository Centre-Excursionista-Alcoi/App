package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.table.ItemTypesTable
import org.centrexcursionistalcoi.app.database.table.ItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.database.table.SectionsTable
import org.centrexcursionistalcoi.app.database.table.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object ServerDatabase {
    private val logger = LoggerFactory.getLogger(ServerDatabase::class.java)

    private var instance: Database? = null

    private val tables = listOf<Table>(
        UsersTable,
        ItemTypesTable,
        ItemsTable,
        LendingsTable,
        LendingItemsTable,
        SectionsTable
    )

    suspend fun initialize(
        url: String = "jdbc:h2:file:./CEA",
        driver: String = "org.h2.Driver"
    ) {
        logger.info("Initializing database at $url with $driver...")
        instance = Database.connect(url, driver)

        invoke {
            for (table in tables) {
                logger.debug("Making sure ${table.tableName} is created...")
                SchemaUtils.create(table)
            }
        }
    }

    suspend operator fun <R> invoke(block: () -> R): R {
        check(instance != null) { "Database is not initialized." }
        return withContext(Dispatchers.IO) {
            transaction(instance) {
                block()
            }
        }
    }
}
