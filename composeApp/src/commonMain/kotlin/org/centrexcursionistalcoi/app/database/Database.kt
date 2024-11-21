package org.centrexcursionistalcoi.app.database

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect val driver: SQLiteDriver

lateinit var roomDatabaseBuilder: RoomDatabase.Builder<AppDatabase>

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase> = roomDatabaseBuilder
): AppDatabase {
    return builder
        .addMigrations()
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(driver)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
