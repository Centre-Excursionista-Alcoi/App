package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.jetbrains.annotations.TestOnly
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.DriverManager
import org.jetbrains.exposed.v1.jdbc.Database as JdbcDatabase

object Database {
    val tables = arrayOf(Files, Departments, Posts)
    private var database: JdbcDatabase? = null

    const val URL = "jdbc:sqlite:file:test?mode=memory&cache=shared" // In-memory SQLite database

    @TestOnly
    const val TEST_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;"

    private fun connect(
        url: String,
        driver: String,
        user: String,
        password: String
    ) = JdbcDatabase.connect(url, driver, user, password)

    fun init(url: String = URL, username: String = "", password: String = "") {
        println("Initializing database with url: $url")
        val driver = DriverManager.getDriver(url).javaClass.name
        println("Using driver: $driver")
        database = connect(url, driver, username, password)

        transaction(database) {
            SchemaUtils.create(*tables)
        }
    }

    @TestOnly
    fun clear() {
        this {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
    }

    operator fun <T> invoke(statement: JdbcTransaction.() -> T): T = transaction(database, statement)
}
