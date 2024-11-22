package org.centrexcursionistalcoi.app.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.centrexcursionistalcoi.app.database.converter.Converters
import org.centrexcursionistalcoi.app.database.dao.AdminDao
import org.centrexcursionistalcoi.app.database.dao.BookingsDao
import org.centrexcursionistalcoi.app.database.dao.InventoryDao
import org.centrexcursionistalcoi.app.database.dao.SpacesDao
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.admin.User

@Database(
    entities = [
        Section::class, ItemType::class, Item::class, Space::class,
        ItemBooking::class, SpaceBooking::class,
        User::class
    ],
    version = 2,
    autoMigrations = [
        // Added users admin entity
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookingsDao(): BookingsDao

    abstract fun inventoryDao(): InventoryDao

    abstract fun spacesDao(): SpacesDao

    abstract fun adminDao(): AdminDao
}
