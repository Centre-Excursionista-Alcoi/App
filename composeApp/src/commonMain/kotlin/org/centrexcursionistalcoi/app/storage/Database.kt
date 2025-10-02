package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver
import org.centrexcursionistalcoi.app.database.Database

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

lateinit var databaseInstance: Database

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    return Database(driver)
}
