package org.centrexcursionistalcoi.app.database

import java.sql.Connection
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Heavy test, use only when strictly necessary.
 */
abstract class PostgresTestBase {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:17").apply {
        withDatabaseName("testdb")
        withUsername("test")
        withPassword("test")
        withReuse(true) // speeds up subsequent test runs
    }

    @BeforeEach
    fun setup() {
        println("Starting PostgreSQL container for tests...")
        postgres.start()

        println("Connecting to PostgreSQL database at ${postgres.jdbcUrl}...")
        Database.initializeConnection(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            username = postgres.username,
            password = postgres.password
        )

        // Optional: ensure Exposed uses transactions automatically
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
    }

    @AfterEach
    fun teardown() {
        println("Stopping PostgreSQL container...")
        postgres.stop()
    }
}
