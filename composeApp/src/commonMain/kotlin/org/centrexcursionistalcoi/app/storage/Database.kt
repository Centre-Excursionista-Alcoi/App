package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.adapters.*
import org.centrexcursionistalcoi.app.database.data.*
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

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
        Departments.Adapter(
            UUIDAdapter,
            UUIDAdapter,
            JsonAdapter(ListSerializer(DepartmentMemberInfo.serializer()))
        ),
        Events.Adapter(
            UUIDAdapter,
            InstantAdapter,
            InstantAdapter,
            UUIDAdapter,
            UUIDAdapter,
            JsonAdapter(ListSerializer(String.serializer()))
        ),
        InventoryItemTypes.Adapter(UUIDAdapter, ListStringAdapter, UUIDAdapter, UUIDAdapter),
        InventoryItems.Adapter(UUIDAdapter, UUIDAdapter),
        LendingItems.Adapter(UUIDAdapter, UUIDAdapter),
        Lendings.Adapter(
            UUIDAdapter,
            InstantAdapter,
            LocalDateAdapter,
            LocalDateAdapter,
            InstantAdapter,
            InstantAdapter,
            JsonAdapter(LendingMemory.serializer()),
            UUIDAdapter,
        ),
        Members.Adapter(
            EnumColumnAdapter()
        ),
        Posts.Adapter(
            UUIDAdapter,
            InstantAdapter,
            UUIDAdapter,
            JsonAdapter(ListSerializer(FileWithContext.serializer()))
        ),
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
