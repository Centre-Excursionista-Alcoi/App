package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.table.ItemTypesTable
import org.centrexcursionistalcoi.app.database.table.ItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.database.table.SectionsTable
import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.centrexcursionistalcoi.app.database.table.SpaceKeysTable
import org.centrexcursionistalcoi.app.database.table.SpacesImagesTable
import org.centrexcursionistalcoi.app.database.table.SpacesTable
import org.centrexcursionistalcoi.app.database.table.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object ServerDatabase {
    private val logger = LoggerFactory.getLogger(ServerDatabase::class.java)

    private var url: String = "jdbc:h2:file:./CEA"
    private var driver: String = "org.h2.Driver"
    private var username: String = ""
    private var password: String = ""

    private val instance: Database by lazy {
        Database.connect(url, driver, username, password)
    }

    private val databaseMutex = Semaphore(1)

    private val tables = listOf<Table>(
        UsersTable,
        ItemTypesTable,
        ItemsTable,
        LendingsTable,
        LendingItemsTable,
        SectionsTable,
        SpacesTable,
        SpacesImagesTable,
        SpaceKeysTable,
        SpaceBookingsTable
    )

    suspend fun initialize(
        url: String = "jdbc:h2:file:./CEA",
        driver: String = "org.h2.Driver",
        username: String = "",
        password: String = ""
    ) {
        logger.info("Initializing database at $url with $driver...")
        this.url = url
        this.driver = driver
        this.username = username
        this.password = password

        invoke {
            for (table in tables) {
                logger.debug("Making sure ${table.tableName} is created...")
                SchemaUtils.create(table)
            }
        }
    }

    suspend operator fun <R> invoke(block: () -> R): R {
        return withContext(Dispatchers.IO) {
            databaseMutex.withPermit {
                transaction(instance) {
                    block()
                }
            }
        }
    }
}
