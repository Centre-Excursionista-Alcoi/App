package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.centrexcursionistalcoi.app.database.Database
import org.w3c.dom.Worker

@OptIn(ExperimentalWasmJsInterop::class)
internal fun jsWorker(): Worker = js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            jsWorker()
        ).also { Database.Schema.awaitCreate(it) }
    }
}
