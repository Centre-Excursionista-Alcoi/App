package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties
import org.centrexcursionistalcoi.app.database.Database

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return JdbcSqliteDriver("jdbc:sqlite:escalaralcoiaicomtat.db", Properties(), Database.Schema.synchronous())
    }
}
