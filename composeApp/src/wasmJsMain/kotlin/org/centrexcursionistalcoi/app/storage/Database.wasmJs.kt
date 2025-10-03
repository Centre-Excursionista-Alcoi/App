package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import org.centrexcursionistalcoi.app.database.Database

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return createDefaultWebWorkerDriver().also { Database.Schema.awaitCreate(it) }
    }
}
