package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.db.SqlDriver
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.adapters.InstantAdapter
import org.centrexcursionistalcoi.app.database.adapters.JsonAdapter
import org.centrexcursionistalcoi.app.database.adapters.ListStringAdapter
import org.centrexcursionistalcoi.app.database.adapters.LocalDateAdapter
import org.centrexcursionistalcoi.app.database.adapters.UUIDAdapter
import org.centrexcursionistalcoi.app.database.data.Departments
import org.centrexcursionistalcoi.app.database.data.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.data.InventoryItems
import org.centrexcursionistalcoi.app.database.data.LendingItems
import org.centrexcursionistalcoi.app.database.data.Lendings
import org.centrexcursionistalcoi.app.database.data.Posts
import org.centrexcursionistalcoi.app.database.data.ReceivedItems
import org.centrexcursionistalcoi.app.database.data.Users

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
        InventoryItemTypes.Adapter(UUIDAdapter, ListStringAdapter, UUIDAdapter),
        InventoryItems.Adapter(UUIDAdapter, UUIDAdapter),
        LendingItems.Adapter(UUIDAdapter, UUIDAdapter),
        Lendings.Adapter(
            UUIDAdapter,
            InstantAdapter,
            LocalDateAdapter,
            LocalDateAdapter,
            InstantAdapter,
            InstantAdapter,
            UUIDAdapter,
        ),
        Posts.Adapter(UUIDAdapter, InstantAdapter),
        ReceivedItems.Adapter(
            UUIDAdapter,
            UUIDAdapter,
            UUIDAdapter,
            InstantAdapter,
        ),
        Users.Adapter(
            JsonAdapter(ListSerializer(String.serializer())),
            JsonAdapter(ListSerializer(DepartmentMemberInfo.serializer())),
            JsonAdapter(LendingUser.serializer()),
            JsonAdapter(ListSerializer(UserInsurance.serializer())),
        )
    )
}
