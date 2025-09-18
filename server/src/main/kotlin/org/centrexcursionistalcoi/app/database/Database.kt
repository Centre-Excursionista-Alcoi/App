package org.centrexcursionistalcoi.app.database

import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.jetbrains.annotations.TestOnly
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object Database {
    val tables = arrayOf(Files, Departments, Posts)
    private var database: R2dbcDatabase? = null

    const val URL = "r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1;" // In-memory H2 database

    @TestOnly
    const val TEST_URL = "r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1;"

    private fun connect(
        url: String = URL,
        user: String = "",
        password: String = ""
    ) = R2dbcDatabase.connect(url, user = user, password = password)

    suspend fun init(url: String = URL, username: String = "", password: String = "") {
        database = connect(url, username, password)

        suspendTransaction(database) {
            SchemaUtils.create(*tables)
        }
    }

    @TestOnly
    suspend fun clear() {
        this {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
    }

    suspend operator fun <T> invoke(statement: suspend R2dbcTransaction.() -> T): T = suspendTransaction(database, statement)
}
