package org.centrexcursionistalcoi.app.database

import java.sql.Connection
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Heavy test, use only when strictly necessary.
 */
abstract class PostgresTestBase {
    companion object {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:17").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withReuse(true) // speeds up subsequent test runs
        }

        @BeforeAll
        @JvmStatic
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

        @AfterAll
        @JvmStatic
        fun teardown() {
            println("Stopping PostgreSQL container...")
            postgres.stop()
        }
    }
}
