package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    suspend fun createDriver(): SqlDriver
}
