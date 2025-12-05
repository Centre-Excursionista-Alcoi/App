package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.migrations.DatabaseMigration
import org.centrexcursionistalcoi.app.database.table.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SchemaUtils.sortTablesByReferences
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.sql.Types
import org.jetbrains.exposed.v1.jdbc.Database as JdbcDatabase

object Database {
    private val tables = listOf(
        Files,
        ConfigTable,
        Departments,
        UserReferences,
        Posts,
        PostFiles,
        LendingUsers,
        DepartmentMembers,
        UserInsurances,
        InventoryItemTypes,
        InventoryItems,
        Lendings,
        LendingItems,
        ReceivedItems,
        RecoverPasswordRequests,
        FCMRegistrationTokens,
        Events,
        EventMembers,
    ).let { sortTablesByReferences(it) }
    private var database: JdbcDatabase? = null

    const val URL = "jdbc:sqlite:file:test?mode=memory&cache=shared" // In-memory SQLite database

    @TestOnly
    const val TEST_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;"

    const val VERSION = 3

    private val logger = LoggerFactory.getLogger(Database::class.java)

    fun isInitialized(): Boolean = database != null

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
        database = JdbcDatabase.connect(url, driver, username, password)
    }

    fun init(
        url: String = URL,
        driver: String? = null,
        username: String = "",
        password: String = "",
    ) {
        if (database == null) {
            initializeConnection(url, driver, username, password)
        }

        logger.info("Creating database schema if not exists")
        for (table in tables) {
            transaction(database) {
                logger.debug("Ensuring table ${table.tableName} exists")
                if (table.exists()) {
                    logger.debug("Table ${table.tableName} exists, skipping creation")
                } else {
                    for (statement in SchemaUtils.createStatements(table)) {
                        try {
                            exec(statement)
                        } catch (e: ExposedSQLException) {
                            if (e.message?.contains("already exists") == true) {
                                // this may also be thrown if an ALTER query is run and it already exists
                                logger.warn("Table ${table.tableName} already exists, skipping creation")
                            } else {
                                throw e
                            }
                        }
                    }
                }
            }
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
        if (database == null) return
        this {
            val tables = tables.reversed().toTypedArray()
            SchemaUtils.drop(*tables)
        }
        database = null
    }

    operator fun <T> invoke(statement: JdbcTransaction.() -> T): T = transaction(database, statement = statement)

    private val sqlIgnoreExecuteFailure = setOf("CREATE TABLE", "ALTER TABLE")

    /**
     * Executes raw SQL queries. ONLY FOR TESTS.
     * @param queries SQL queries to execute. They will be run on sepparate transactions.
     * @return true if all queries executed successfully, false otherwise.
     */
    @TestOnly
    @VisibleForTesting
    @Suppress("SqlSourceToSinkFlow")
    internal fun exec(vararg queries: String) = queries.all { query ->
        transaction(database) {
            val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection
            val statement = conn.createStatement()
            logger.info("Executing SQL: $query")
            statement.execute(query)
                .let { success ->
                    if (success) {
                        logger.info("SQL executed successfully. Update count: ${statement.updateCount}")
                        true
                    } else if (sqlIgnoreExecuteFailure.any { query.contains(it, true) }) {
                        logger.warn("SQL execution failed, but query is a CREATE statement. This might be because CREATE doesn't update any rows, so update count is 0. Trusting that it succeeded.")
                        true
                    } else {
                        logger.error("SQL execution failed.")
                        false
                    }
                }
        }
    }

    /**
     * Executes raw SQL queries with arguments. ONLY FOR TESTS.
     * @param query SQL query to execute.
     * @param args Arguments for the SQL query.
     * @return true if the query executed successfully, false otherwise.
     */
    @TestOnly
    @VisibleForTesting
    @Suppress("SqlSourceToSinkFlow")
    internal fun exec(query: String, args: Array<Any?>) = transaction(database) {
        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        val statement = conn.prepareStatement(query)
        for ((idx, arg) in args.withIndex()) {
            when (arg) {
                is Int -> statement.setInt(idx + 1, arg)
                is String -> statement.setString(idx + 1, arg)
                is Long -> statement.setLong(idx + 1, arg)
                is Boolean -> statement.setBoolean(idx + 1, arg)
                is Float -> statement.setFloat(idx + 1, arg)
                is Double -> statement.setDouble(idx + 1, arg)
                is ByteArray -> statement.setBytes(idx + 1, arg)
                is Array<*> -> statement.setArray(
                    idx + 1,
                    conn.createArrayOf(
                        when {
                            arg.isEmpty() -> "text" // default to text[] for empty arrays
                            arg[0] is String -> "text"
                            arg[0] is Int -> "integer"
                            arg[0] is Long -> "bigint"
                            else -> error("Unsupported array argument type: ${arg[0]?.javaClass?.name}")
                        },
                        arg,
                    ),
                )
                null -> statement.setNull(idx + 1, Types.NULL)
                else -> error("Unsupported argument type: ${arg.javaClass.name}")
            }
        }
        logger.info("Executing SQL (${args.joinToString()}): $query")
        statement.executeUpdate() > 0
    }

    @TestOnly
    @VisibleForTesting
    @Suppress("SqlSourceToSinkFlow")
    internal fun execQuery(query: String) = transaction(database) {
        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        val statement = conn.createStatement()
        logger.info("Executing SQL query: $query")
        statement.executeQuery(query)
    }
}
