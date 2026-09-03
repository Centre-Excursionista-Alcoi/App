package org.centrexcursionistalcoi.app.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import org.centrexcursionistalcoi.app.database.room.RoomConverters
import org.centrexcursionistalcoi.app.database.room.dao.DepartmentDao
import org.centrexcursionistalcoi.app.database.room.dao.EventDao
import org.centrexcursionistalcoi.app.database.room.dao.EventUserCrossRefDao
import org.centrexcursionistalcoi.app.database.room.dao.InventoryItemDao
import org.centrexcursionistalcoi.app.database.room.dao.InventoryItemTypeDao
import org.centrexcursionistalcoi.app.database.room.dao.LendingDao
import org.centrexcursionistalcoi.app.database.room.dao.LendingItemDao
import org.centrexcursionistalcoi.app.database.room.dao.MemberDao
import org.centrexcursionistalcoi.app.database.room.dao.MemoryDao
import org.centrexcursionistalcoi.app.database.room.dao.MemoryMemberCrossRefDao
import org.centrexcursionistalcoi.app.database.room.dao.PostDao
import org.centrexcursionistalcoi.app.database.room.dao.ReceivedItemDao
import org.centrexcursionistalcoi.app.database.room.dao.UserDao
import org.centrexcursionistalcoi.app.database.room.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.room.entity.EventEntity
import org.centrexcursionistalcoi.app.database.room.entity.EventUserCrossRef
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemoryMemberCrossRef
import org.centrexcursionistalcoi.app.database.room.entity.PostEntity
import org.centrexcursionistalcoi.app.database.room.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.UserEntity

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
    abstract fun departmentDao(): DepartmentDao
    abstract fun eventDao(): EventDao
    abstract fun eventUserCrossRefDao(): EventUserCrossRefDao
    abstract fun inventoryItemTypeDao(): InventoryItemTypeDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun lendingDao(): LendingDao
    abstract fun lendingItemDao(): LendingItemDao
    abstract fun memberDao(): MemberDao
    abstract fun memoryDao(): MemoryDao
    abstract fun memoryMemberCrossRefDao(): MemoryMemberCrossRefDao
    abstract fun postDao(): PostDao
    abstract fun receivedItemDao(): ReceivedItemDao
    abstract fun userDao(): UserDao
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
