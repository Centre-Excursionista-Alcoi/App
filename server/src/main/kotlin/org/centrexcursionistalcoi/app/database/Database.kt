package org.centrexcursionistalcoi.app.database

import java.sql.DriverManager
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.migrations.DatabaseMigration
import org.centrexcursionistalcoi.app.database.table.ConfigTable
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.database.table.ReceivedItems
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import org.jetbrains.exposed.v1.jdbc.Database as JdbcDatabase

object Database {
    val tables = arrayOf(
        Files,
        ConfigTable,
        Departments,
        UserReferences,
        Posts,
        LendingUsers,
        DepartmentMembers,
        UserInsurances,
        InventoryItemTypes,
        InventoryItems,
        Lendings,
        LendingItems,
        ReceivedItems,
        FCMRegistrationTokens,
    )
    private var database: JdbcDatabase? = null

    const val URL = "jdbc:sqlite:file:test?mode=memory&cache=shared" // In-memory SQLite database

    @TestOnly
    const val TEST_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;"

    const val VERSION = 1

    private val logger = LoggerFactory.getLogger(Database::class.java)

    fun isInitialized(): Boolean = database != null

    private fun connect(
        url: String,
        driver: String,
        user: String,
        password: String
    ) = JdbcDatabase.connect(url, driver, user, password)

    @TestOnly
    @VisibleForTesting
    fun initializeConnection(
        url: String = URL,
        driver: String? = null,
        username: String = "",
        password: String = "",
    ) {
        logger.info("Initializing database with url: $url")
        val driver = driver ?: DriverManager.getDriver(url).javaClass.name
        logger.debug("Using driver: $driver")
        database = connect(url, driver, username, password)
    }

    fun init(
        url: String = URL,
        driver: String? = null,
        username: String = "",
        password: String = "",
    ) {
        initializeConnection(url, driver, username, password)

        logger.info("Creating database schema if not exists")
        transaction(database) {
            SchemaUtils.create(*tables)
        }

        val version = Database { ConfigEntity[ConfigEntity.DatabaseVersion] }
        if (version == null) {
            logger.info("Setting database version to $VERSION")
            Database {
                ConfigEntity[ConfigEntity.DatabaseVersion] = VERSION
            }
        } else if (version < VERSION) {
            error("Database version $version is older than application version $VERSION. Migration not implemented.")
        } else if (version > VERSION) {
            var migration = DatabaseMigration.next(version)
            if (migration != null) {
                logger.info("Database version $version is newer than application version $VERSION. Running migration...")
                while (migration != null) {
                    logger.info("Migrating database from version ${migration.from} to ${migration.to}")
                    Database {
                        migration!!.migrate()
                        ConfigEntity[ConfigEntity.DatabaseVersion] = migration!!.to
                    }
                    migration = DatabaseMigration.next(migration.to)
                }
                logger.info("Database migration completed. Current version is now $VERSION.")
            } else {
                error("No migration found from database version $version to application version $VERSION.")
            }
        }
    }

    @TestOnly
    fun clear() {
        this {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
    }

    operator fun <T> invoke(statement: JdbcTransaction.() -> T): T = transaction(database, statement = statement)

    @TestOnly
    @VisibleForTesting
    @Suppress("SqlSourceToSinkFlow")
    @Deprecated("Should not be used. Only for tests.")
    internal fun exec(query: String) = transaction(database) {
        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        val statement = conn.createStatement()
        statement.execute(query)
    }

    @TestOnly
    @VisibleForTesting
    @Suppress("SqlSourceToSinkFlow")
    @Deprecated("Should not be used. Only for tests.")
    internal fun execQuery(query: String) = transaction(database) {
        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        val statement = conn.createStatement()
        statement.executeQuery(query)
    }
}
