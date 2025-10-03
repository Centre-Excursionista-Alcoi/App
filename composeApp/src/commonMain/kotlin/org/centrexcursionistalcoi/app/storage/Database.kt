package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.adapters.InstantAdapter
import org.centrexcursionistalcoi.app.database.adapters.UUIDAdapter
import org.centrexcursionistalcoi.app.database.data.Departments
import org.centrexcursionistalcoi.app.database.data.Posts

expect class DriverFactory {
    suspend fun createDriver(): SqlDriver
}

lateinit var databaseInstance: Database

val isDatabaseReady: Boolean
    get() = ::databaseInstance.isInitialized

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
suspend fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    return Database(
        driver,
        Departments.Adapter(UUIDAdapter),
        Posts.Adapter(UUIDAdapter, InstantAdapter)
    )
}
