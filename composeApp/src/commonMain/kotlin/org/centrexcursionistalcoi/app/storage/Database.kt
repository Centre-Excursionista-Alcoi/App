package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.adapters.InstantAdapter
import org.centrexcursionistalcoi.app.database.adapters.UUIDAdapter
import org.centrexcursionistalcoi.app.database.data.Posts
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

lateinit var databaseInstance: Database

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    return Database(
        driver,
        Posts.Adapter(UUIDAdapter, InstantAdapter)
    )
}
