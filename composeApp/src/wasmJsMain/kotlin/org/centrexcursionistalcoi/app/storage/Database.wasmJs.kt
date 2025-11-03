package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        throw NotImplementedError("SQLDelight is not supported on WASM yet.")
        // return createDefaultWebWorkerDriver().also { Database.Schema.awaitCreate(it) }
    }
}
