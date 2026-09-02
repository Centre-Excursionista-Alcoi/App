package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.centrexcursionistalcoi.app.database.Database
import java.util.Properties

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return JdbcSqliteDriver("jdbc:sqlite:centrexcursionistalcoi.db", Properties(), Database.Schema.synchronous())
    }
}
