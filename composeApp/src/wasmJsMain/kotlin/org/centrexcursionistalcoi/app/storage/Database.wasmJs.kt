package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            Worker("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js")
        )
    }
}
