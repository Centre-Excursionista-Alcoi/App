package org.centrexcursionistalcoi.app.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.EventUserCrossRef
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingItemEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryMemberCrossRef
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.UserEntity

const val DATABASE_VERSION = 1
const val DATABASE_FILE_NAME = "cea_app.db"

@Database(
    entities = [
        DepartmentEntity::class,
        EventEntity::class,
        EventUserCrossRef::class,
        InventoryItemTypeEntity::class,
        InventoryItemEntity::class,
        LendingEntity::class,
        LendingItemEntity::class,
        MemberEntity::class,
        MemoryEntity::class,
        MemoryMemberCrossRef::class,
        PostEntity::class,
        ReceivedItemEntity::class,
        UserEntity::class,
    ],
    version = DATABASE_VERSION,
)
@ColumnTypeConverters(RoomConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun departmentDao(): org.centrexcursionistalcoi.app.database.dao.DepartmentDao
    abstract fun eventDao(): org.centrexcursionistalcoi.app.database.dao.EventDao
    abstract fun eventUserCrossRefDao(): org.centrexcursionistalcoi.app.database.dao.EventUserCrossRefDao
    abstract fun inventoryItemTypeDao(): org.centrexcursionistalcoi.app.database.dao.InventoryItemTypeDao
    abstract fun inventoryItemDao(): org.centrexcursionistalcoi.app.database.dao.InventoryItemDao
    abstract fun lendingDao(): org.centrexcursionistalcoi.app.database.dao.LendingDao
    abstract fun lendingItemDao(): org.centrexcursionistalcoi.app.database.dao.LendingItemDao
    abstract fun memberDao(): org.centrexcursionistalcoi.app.database.dao.MemberDao
    abstract fun memoryDao(): org.centrexcursionistalcoi.app.database.dao.MemoryDao
    abstract fun memoryMemberCrossRefDao(): org.centrexcursionistalcoi.app.database.dao.MemoryMemberCrossRefDao
    abstract fun postDao(): org.centrexcursionistalcoi.app.database.dao.PostDao
    abstract fun receivedItemDao(): org.centrexcursionistalcoi.app.database.dao.ReceivedItemDao
    abstract fun userDao(): org.centrexcursionistalcoi.app.database.dao.UserDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
    dispatcher: CoroutineDispatcher,
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(dispatcher)
        // The local database is a disposable cache resynced from the server (see SyncAllDataBackgroundJob), so a
        // schema version bump can just wipe and let the next sync repopulate it, rather than needing a real migration.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
