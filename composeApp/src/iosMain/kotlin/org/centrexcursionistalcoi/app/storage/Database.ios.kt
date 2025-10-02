package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.centrexcursionistalcoi.app.database.Database

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema.synchronous(), "centrexcursionistalcoi.db")
    }
}